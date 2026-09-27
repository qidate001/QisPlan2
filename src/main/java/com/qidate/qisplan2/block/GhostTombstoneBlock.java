package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GhostTombstoneBlock
        extends HorizontalDirectionalBlock {

    public static final MapCodec<GhostTombstoneBlock> CODEC =
            simpleCodec(GhostTombstoneBlock::new);

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public enum Part implements StringRepresentable {

        LOWER("lower"),
        UPPER("upper");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Part> PART =
            EnumProperty.create(
                    "part",
                    Part.class
            );

    /*
     * 下半部分碰撞箱。
     */
    private static final VoxelShape LOWER_NORTH_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    14.0 / 16.0,
                    1.0,
                    3.0 / 16.0
            );

    private static final VoxelShape LOWER_SOUTH_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    13.0 / 16.0,
                    14.0 / 16.0,
                    1.0,
                    14.0 / 16.0
            );

    private static final VoxelShape LOWER_WEST_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    3.0 / 16.0,
                    1.0,
                    14.0 / 16.0
            );

    private static final VoxelShape LOWER_EAST_SHAPE =
            Shapes.box(
                    13.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    14.0 / 16.0,
                    1.0,
                    14.0 / 16.0
            );

    /*
     * 上半部分碰撞箱。
     */
    private static final VoxelShape UPPER_NORTH_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    14.0 / 16.0,
                    14.0 / 16.0,
                    3.0 / 16.0
            );

    private static final VoxelShape UPPER_SOUTH_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    13.0 / 16.0,
                    14.0 / 16.0,
                    14.0 / 16.0,
                    14.0 / 16.0
            );

    private static final VoxelShape UPPER_WEST_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    3.0 / 16.0,
                    14.0 / 16.0,
                    14.0 / 16.0
            );

    private static final VoxelShape UPPER_EAST_SHAPE =
            Shapes.box(
                    13.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    14.0 / 16.0,
                    14.0 / 16.0,
                    14.0 / 16.0
            );

    public GhostTombstoneBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
                        .setValue(
                                PART,
                                Part.LOWER
                        )
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                PART
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {

        BlockPos pos =
                context.getClickedPos();

        BlockPos upperPos =
                pos.above();

        /*
         * 第二个方块放不下，
         * 整个墓碑就不允许放置。
         */
        if (!context.getLevel()
                .getBlockState(upperPos)
                .canBeReplaced(context)) {

            return null;
        }

        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                )
                .setValue(
                        PART,
                        Part.LOWER
                );
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            LivingEntity placer,
            ItemStack stack
    ) {

        super.setPlacedBy(
                level,
                pos,
                state,
                placer,
                stack
        );

        if (level.isClientSide()) {
            return;
        }

        /*
         * 放置上半部分。
         */
        level.setBlock(
                pos.above(),
                state.setValue(
                        PART,
                        Part.UPPER
                ),
                3
        );
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {

        if (!level.isClientSide()) {

            if (state.getValue(PART)
                    == Part.LOWER) {

                destroyOtherHalf(
                        level,
                        pos,
                        state,
                        true
                );

            } else {

                destroyOtherHalf(
                        level,
                        pos,
                        state,
                        false
                );
            }
        }

        return super.playerWillDestroy(
                level,
                pos,
                state,
                player
        );
    }

    private void destroyOtherHalf(
            Level level,
            BlockPos pos,
            BlockState state,
            boolean lower
    ) {

        BlockPos otherPos;

        if (lower) {
            otherPos = pos.above();
        } else {
            otherPos = pos.below();
        }

        BlockState otherState =
                level.getBlockState(
                        otherPos
                );

        if (otherState.is(this)) {

            level.destroyBlock(
                    otherPos,
                    false
            );
        }
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {

        boolean upper =
                state.getValue(PART)
                        == Part.UPPER;

        return switch (state.getValue(FACING)) {

            case SOUTH ->
                    upper
                            ? UPPER_SOUTH_SHAPE
                            : LOWER_SOUTH_SHAPE;

            case WEST ->
                    upper
                            ? UPPER_WEST_SHAPE
                            : LOWER_WEST_SHAPE;

            case EAST ->
                    upper
                            ? UPPER_EAST_SHAPE
                            : LOWER_EAST_SHAPE;

            default ->
                    upper
                            ? UPPER_NORTH_SHAPE
                            : LOWER_NORTH_SHAPE;
        };
    }
}