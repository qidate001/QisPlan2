package com.qidate.qisplan2.ghost.ability.knockingghost;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.GhostDoorBlock;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.GhostAbilityContext;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.AABB;

public final class KnockingGhostAbility
        implements PossessedGhostAbility {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "knocking_ghost"
            );

    /**
     * 10 秒。
     */
    private static final long TEN_SECONDS = 200L;

    /**
     * 普通敲门：
     *
     * +10%
     */
    private static final double NORMAL_REVIVAL_GAIN = 10.0D;

    /**
     * 10 秒内再次敲门：
     *
     * +30%
     */
    private static final double RAPID_REVIVAL_GAIN = 30.0D;

    /**
     * 每个受到袭击的目标：
     *
     * +2%
     */
    private static final double TARGET_REVIVAL_BONUS = 2.0D;

    /**
     * 敲门攻击范围。
     *
     * 与敲门鬼本体保持一致：
     * 16 格。
     */
    private static final double KNOCK_RANGE = 16.0D;

    /**
     * 敲门灵异攻击强度。
     */
    private static final double KNOCK_ATTACK_STRENGTH = 5.0D;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /**
     * 敲门鬼目前没有 LivingEntity 主动能力。
     */
    @Override
    public boolean use(
            GhostAbilityContext context
    ) {
        return false;
    }

    /**
     * Shift + 右键门。
     */
    @Override
    public boolean useOnBlock(
            GhostAbilityContext context,
            BlockPos clickedPos
    ) {

        ServerPlayer player =
                context.player();

        if (!(player.level()
                instanceof ServerLevel serverLevel)) {

            return false;
        }

        /*
         * ========================================================
         * 找到门的下半部分
         * ========================================================
         */
        BlockPos doorPos =
                getLowerDoorPos(
                        serverLevel,
                        clickedPos
                );

        if (doorPos == null) {
            return false;
        }

        /*
         * ========================================================
         * 判断是否为鬼门
         * ========================================================
         */
        boolean ghostDoor =
                isGhostDoor(
                        serverLevel,
                        doorPos
                );

        /*
         * ========================================================
         * 找出敲门范围内所有袭击目标
         * ========================================================
         */
        int targetCount = 0;

        AABB attackBox =
                new AABB(
                        doorPos
                ).inflate(
                        KNOCK_RANGE
                );

        for (LivingEntity entity :
                serverLevel.getEntitiesOfClass(
                        LivingEntity.class,
                        attackBox,
                        LivingEntity::isAlive
                )) {

            /*
             * 不攻击自己。
             */
            if (entity == player) {
                continue;
            }

            /*
             * 使用真正的球形距离，
             * 防止 AABB 角落里的实体被算进去。
             */
            double dx =
                    entity.getX()
                            - (doorPos.getX() + 0.5D);

            double dy =
                    entity.getY()
                            - (doorPos.getY() + 0.5D);

            double dz =
                    entity.getZ()
                            - (doorPos.getZ() + 0.5D);

            if (dx * dx
                    + dy * dy
                    + dz * dz
                    > KNOCK_RANGE
                    * KNOCK_RANGE) {

                continue;
            }

            targetCount++;

            /*
             * ====================================================
             * 灵异袭击
             * ====================================================
             */
            SupernaturalDeathHandler.tryKill(
                    entity,
                    ModDamageTypes.knockingGhost(
                            player
                    ),
                    KNOCK_ATTACK_STRENGTH
            );
        }

        /*
         * ========================================================
         * 计算本次复苏增长
         * ========================================================
         */

        PossessedGhostState state =
                context.state();

        long now =
                player.serverLevel()
                        .getGameTime();

        long lastUse =
                state.lastAbilityUseTick();

        /*
         * 10 秒内再次敲门：
         * 基础复苏从 10% 变成 30%。
         *
         * 这里额外目标奖励仍然是：
         * 每个目标 +2%。
         *
         * 因此：
         *
         * 1 个目标：
         * 32%
         *
         * 5 个目标：
         * 40%
         */
        boolean rapid =
                lastUse > 0
                        && now - lastUse < TEN_SECONDS;

        double baseGain =
                rapid
                        ? RAPID_REVIVAL_GAIN
                        : NORMAL_REVIVAL_GAIN;

        double revivalGain =
                baseGain
                        + targetCount
                        * TARGET_REVIVAL_BONUS;

        /*
         * ========================================================
         * 加入复苏值
         * ========================================================
         */
        PossessedGhostState newState =
                PossessionHandler.addRevival(
                        state,
                        revivalGain
                );

        /*
         * ========================================================
         * 记录本次敲门时间。
         * ========================================================
         */
        newState =
                new PossessedGhostState(
                        newState.revival(),
                        newState.shallowStun(),
                        newState.stunTicks(),
                        newState.permanentStun(),
                        now
                );

        context.setState(
                newState
        );

        /*
         * ========================================================
         * 播放敲门声
         * ========================================================
         */
        serverLevel.playSound(
                null,
                doorPos,
                QisPlan2.GHOST_KNOCK.get(),
                SoundSource.HOSTILE,
                1.0F,
                1.0F
        );

        /*
         * ========================================================
         * 普通门：
         *
         * 敲一次就损毁。
         *
         * 鬼门：
         * 不损毁。
         * ========================================================
         */
        if (!ghostDoor) {

            serverLevel.destroyBlock(
                    doorPos,
                    false
            );
        }

        /*
         * 如果这次已经涨到 100%，
         * 直接死亡。
         */
        if (newState.revival()
                >= 1.0D) {

            player.setHealth(0.0F);
        }

        return true;
    }

    /**
     * 获取门的下半部分。
     */
    private static BlockPos getLowerDoorPos(
            ServerLevel level,
            BlockPos clickedPos
    ) {

        BlockState state =
                level.getBlockState(
                        clickedPos
                );

        /*
         * ========================================================
         * 原版 DoorBlock
         * ========================================================
         */
        if (state.getBlock()
                instanceof DoorBlock) {

            if (!state.hasProperty(
                    DoorBlock.HALF
            )) {
                return null;
            }

            if (state.getValue(
                    DoorBlock.HALF
            ) == DoubleBlockHalf.UPPER) {

                return clickedPos.below();
            }

            return clickedPos;
        }

        /*
         * ========================================================
         * 其他标签门
         * ========================================================
         */
        if (state.is(BlockTags.DOORS)) {

            if (state.hasProperty(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF
            )) {

                if (state.getValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF
                ) == DoubleBlockHalf.UPPER) {

                    return clickedPos.below();
                }
            }

            return clickedPos;
        }

        return null;
    }

    /**
     * 判断是不是鬼门。
     */
    private static boolean isGhostDoor(
            ServerLevel level,
            BlockPos pos
    ) {

        return level.getBlockState(pos)
                .getBlock()
                instanceof GhostDoorBlock;
    }
}