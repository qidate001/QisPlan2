package com.qidate.qisplan2.ghost.ability.knockingghost;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModEntities;
import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.ghost.GhostAbilityContext;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.PossessedGhostAbility;

import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.tags.BlockTags;

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
     * 灵异侵蚀
     */
    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(CorrosionType.GLOBAL, 20)
                    .add(CorrosionType.HAND, 20)
                    .build();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public EntityType<? extends AbstractGhostEntity> entityType() {
        return ModEntities.KNOCKING_GHOST.get();
    }

    /**
     * 敲门鬼完整复苏时的本质灵异强度。
     */
    @Override
    public double initialIntrinsicStrength() {
        return 20.0D;
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
     * 灵异侵蚀
     */
    @Override
    public GhostCorrosion corrosion() {
        return CORROSION;
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
         * 计算这一次“直接敲下这扇门”
         * 有多少生物听见。
         *
         * 注意：
         *
         * 鬼门后续传播产生的其他门，
         * 不在这里重复增加驾驭者复苏。
         * ========================================================
         */

        int targetCount =
                KnockingGhostDoorSystem.countHearingTargets(
                        serverLevel,
                        player,
                        doorPos
                );


        /*
         * ========================================================
         * 真正执行敲门。
         *
         * 包括：
         *
         * 声音
         * 延迟攻击
         * 鬼门
         * 50 格共鸣
         * 50 层递归
         * ========================================================
         */

        double attackStrength =
                PossessionHandler.getEffectiveStrength(
                        player,
                        ID
                );

        KnockingGhostDoorSystem.knock(
                serverLevel,
                player,
                doorPos,
                attackStrength
        );


        /*
         * ========================================================
         * 驾驭代价
         * ========================================================
         */

        PossessedGhostState state =
                context.state();

        long now =
                serverLevel.getGameTime();

        boolean rapid =
                state.lastAbilityUseTick() > 0
                        && now - state.lastAbilityUseTick()
                        < TEN_SECONDS;


        double baseGain =
                rapid
                        ? RAPID_REVIVAL_GAIN
                        : NORMAL_REVIVAL_GAIN;


        double revivalGain =
                baseGain
                        + targetCount
                        * TARGET_REVIVAL_BONUS;


        PossessedGhostState newState =
                PossessionHandler.addRevival(
                        player,
                        ID,
                        state,
                        revivalGain,
                        CorrosionType.HAND
                );


        /*
         * 记录能力使用时间。
         */

        newState =
                new PossessedGhostState(
                        newState.revival(),
                        newState.shallowStun(),
                        newState.stunTicks(),
                        newState.permanentStun(),
                        now,
                        state.intrinsicStrength()
                );

        context.setState(
                newState
        );


        /*
         * ========================================================
         * 普通门：
         *
         * 敲一次就毁掉。
         *
         * 鬼门：
         * 不毁。
         * ========================================================
         */

        if (!KnockingGhostDoorSystem.isGhostDoor(
                serverLevel,
                doorPos
        )) {

            serverLevel.destroyBlock(
                    doorPos,
                    false
            );
        }


        /*
         * ========================================================
         * 复苏到 100%
         * ========================================================
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
}