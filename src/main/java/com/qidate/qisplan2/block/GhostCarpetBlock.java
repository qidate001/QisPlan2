package com.qidate.qisplan2.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class GhostCarpetBlock extends CarpetBlock {

    public GhostCarpetBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);
    }


    /**
     * 方块被放置时检查是否需要下落。
     */
    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(
                state,
                level,
                pos,
                oldState,
                movedByPiston
        );

        checkFall(level, pos, state);
    }


    /**
     * 邻居方块发生变化时检查是否需要下落。
     */
    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos neighborPos,
            boolean movedByPiston
    ) {
        super.neighborChanged(
                state,
                level,
                pos,
                block,
                neighborPos,
                movedByPiston
        );

        checkFall(level, pos, state);
    }


    /**
     * 检查鬼地毯下方是否有支撑。
     */
    private void checkFall(
            Level level,
            BlockPos pos,
            BlockState state
    ) {

        if (level.isClientSide()) {
            return;
        }

        BlockPos below = pos.below();

        if (canFallThrough(level, below)) {

            FallingBlockEntity.fall(
                    level,
                    pos,
                    state
            );
        }
    }


    /**
     * 判断下面的方块是否允许鬼地毯掉落。
     */
    private static boolean canFallThrough(
            Level level,
            BlockPos pos
    ) {

        BlockState state =
                level.getBlockState(pos);

        /*
         * 空气
         */
        if (state.isAir()) {
            return true;
        }

        /*
         * 液体
         */
        return state.liquid();
    }


    /**
     * 鬼地毯无法被正常挖掘。
     */
    @Override
    public float getDestroyProgress(
            BlockState state,
            Player player,
            BlockGetter level,
            BlockPos pos
    ) {
        return 0.0F;
    }
}