package com.qidate.qisplan2.ghost.module.host.player.binding;
import com.qidate.qisplan2.ghost.module.implementation.knocking.KnockingGhostDoorSystem;

import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import static com.qidate.qisplan2.QisPlan2.MODID;
import com.qidate.qisplan2.ghost.module.implementation.knocking.KnockingGhostModule;

/** 玩家承载方式的生命周期和输入适配，不声明厉鬼身份、规则或初始数据。 */
public final class KnockingGhostPlayerBinding implements GhostPlayerBinding {
    private static final ResourceLocation ID = KnockingGhostModule.ID;
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

    @Override
    public boolean use(
            PlayerGhostModuleContext context
    ) {
        return false;
    }

    /**
     * 处理针对方块的主动输入，校验目标后调用对应能力。
     * @return 本次方块操作是否成功处理
     */
    @Override
    public boolean useOnBlock(
            PlayerGhostModuleContext context,
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

        if (!GhostAbilityExecutor.tryExecute(new GhostAbilityContext(context.host(), ID,
                KnockingGhostModule.KNOCK, GhostAbilityContext.Source.POSSESSION, null,
                () -> {}, new DoorKnockParameters(doorPos)))) return false;

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
                        SpiritualPart.HAND
                );


        /*
         * 记录能力使用时间。
         */

        newState =
                new PossessedGhostState(
                        newState.revival(),
                        state.strength(),
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
     * 把点击位置归一化为门的下半部分。
     * 兼容门方块和门标签；位置不是可识别的门时返回 null。
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
                    BlockStateProperties.DOUBLE_BLOCK_HALF
            )) {

                if (state.getValue(
                        BlockStateProperties.DOUBLE_BLOCK_HALF
                ) == DoubleBlockHalf.UPPER) {

                    return clickedPos.below();
                }
            }

            return clickedPos;
        }

        return null;
    }
}
