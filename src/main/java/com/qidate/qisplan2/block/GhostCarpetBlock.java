package com.qidate.qisplan2.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GhostCarpetBlock extends Block
        implements SimpleWaterloggedBlock {

    /**
     * 水logged状态
     */
    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    /**
     * 鬼地毯形状
     *
     * 高度只有 1/16 方块。
     */
    private static final VoxelShape SHAPE =
            Block.box(
                    0,
                    0,
                    0,
                    16,
                    1,
                    16
            );

    public GhostCarpetBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);

        /*
         * 默认状态：
         * 不含水。
         */
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(
                                WATERLOGGED,
                                false
                        )
        );
    }

    /**
     * 注册 BlockState 属性。
     */
    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(WATERLOGGED);
    }

    /**
     * 鬼地毯的形状。
     */
    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    /**
     * 放置时检查当前位置是否有水。
     *
     * 如果有水：
     * ghost_carpet[waterlogged=true]
     *
     * 而不是被水直接替换。
     */
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        FluidState fluidState =
                context.getLevel().getFluidState(
                        context.getClickedPos()
                );

        return defaultBlockState()
                .setValue(
                        WATERLOGGED,
                        fluidState.getType() == Fluids.WATER
                );
    }

    /**
     * 返回鬼地毯当前位置的流体状态。
     *
     * waterlogged 时，这个位置同时存在水。
     */
    @Override
    protected FluidState getFluidState(
            BlockState state
    ) {
        if (state.getValue(WATERLOGGED)) {
            return Fluids.WATER.getSource(false);
        }

        return super.getFluidState(state);
    }

    /**
     * 邻居方块变化时保持水logged状态。
     */
    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(
                    pos,
                    Fluids.WATER,
                    Fluids.WATER.getTickDelay(level)
            );
        }

        return super.updateShape(
                state,
                direction,
                neighborState,
                level,
                pos,
                neighborPos
        );
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
     * 判断下面是否可以让鬼地毯掉落。
     *
     * 现在只有空气会导致掉落。
     *
     * 水、岩浆、其他方块都算支撑。
     */
    private static boolean canFallThrough(
            Level level,
            BlockPos pos
    ) {
        return level.getBlockState(pos).isAir();
    }

    /**
     * 鬼地毯不能被流体替换。
     */
    @Override
    protected boolean canBeReplaced(
            BlockState state,
            Fluid fluid
    ) {
        return false;
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