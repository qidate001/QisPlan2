package com.qidate.qisplan2.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

public class GhostDoorBlock extends DoorBlock {
    private static final int MIN_CLOSE_TICKS = 15 * 20;
    private static final int MAX_CLOSE_TICKS = 40 * 20;

    public GhostDoorBlock(
            BlockSetType blockSetType,
            Properties properties
    ) {
        super(blockSetType, properties);
    }

    /**
     * 鬼门只能从正面打开。
     */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {

        /*
         * 只能从正面打开
         */
        if (!isPlayerOnFront(state, pos, player)) {
            return InteractionResult.PASS;
        }

        /*
         * 原版门开关逻辑
         */
        InteractionResult result =
                super.useWithoutItem(
                        state,
                        level,
                        pos,
                        player,
                        hit
                );

        /*
         * 只有服务端处理自动关闭。
         */
        if (!level.isClientSide()
                && result.consumesAction()) {

            BlockState newState =
                    level.getBlockState(pos);

            /*
             * 确认门现在已经打开。
             */
            if (newState.hasProperty(OPEN)
                    && newState.getValue(OPEN)) {

                /*
                 * 60～120 秒随机关闭。
                 */
                int delay =
                        MIN_CLOSE_TICKS
                                + level.random.nextInt(
                                MAX_CLOSE_TICKS
                                        - MIN_CLOSE_TICKS
                                        + 1
                        );

                BlockPos doorPos = pos;

                if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                    doorPos = pos.below();
                }

                level.scheduleTick(
                        doorPos,
                        this,
                        delay
                );
            }
        }

        return result;
    }

    @Override
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        /*
         * 门已经关闭了。
         * 说明可能是玩家手动关掉的，
         * 这个 Scheduled Tick 直接失效。
         */
        if (!state.getValue(OPEN)) {
            return;
        }

        /*
         * 自动关闭。
         */
        level.setBlock(
                pos,
                state.setValue(
                        OPEN,
                        false
                ),
                Block.UPDATE_ALL
        );

        /*
         * 鬼门关闭音效
         */
        level.playSound(
                null,
                pos,
                SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS,
                8.0F,
                0.5F
        );
    }


    /**
     * 判断玩家是否位于鬼门正面。
     */
    private static boolean isPlayerOnFront(
            BlockState state,
            BlockPos pos,
            Player player
    ) {

        /*
         * 门的朝向。
         */
        var facing =
                state.getValue(FACING);


        /*
         * 门中心。
         */
        double centerX =
                pos.getX() + 0.5;

        double centerZ =
                pos.getZ() + 0.5;


        /*
         * 玩家相对于门中心的位置。
         */
        double dx =
                player.getX() - centerX;

        double dz =
                player.getZ() - centerZ;


        /*
         * 点积判断正反面。
         */
        double dot =
                dx * facing.getStepX()
                        + dz * facing.getStepZ();


        return dot < 0;
    }
}