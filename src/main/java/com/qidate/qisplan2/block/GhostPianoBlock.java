package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GhostPianoBlock extends HorizontalDirectionalBlock {

    public enum Part implements StringRepresentable {
        LEFT("left"),
        RIGHT("right");

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

    public static final MapCodec<GhostPianoBlock> CODEC =
            simpleCodec(GhostPianoBlock::new);

    private static final VoxelShape PIANO_SHAPE =
            box(
                    0,
                    0,
                    0,
                    16,
                    22,
                    16
            );

    public GhostPianoBlock(
            Properties properties
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
                                Part.LEFT
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
            net.minecraft.world.item.context.BlockPlaceContext context
    ) {
        Direction facing =
                context.getHorizontalDirection();

        BlockPos pos =
                context.getClickedPos();

        Direction right =
                facing.getClockWise();

        BlockPos otherPos =
                pos.relative(right);

        /*
         * 另一半位置必须可以替换。
         */
        if (!context.getLevel()
                .getBlockState(otherPos)
                .canBeReplaced(context)) {

            return null;
        }

        return defaultBlockState()
                .setValue(
                        FACING,
                        facing
                )
                .setValue(
                        PART,
                        Part.LEFT
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

        Direction facing =
                state.getValue(FACING);

        Direction right =
                facing.getClockWise();

        BlockPos otherPos =
                pos.relative(right);

        /*
         * 放置右半边。
         */
        level.setBlock(
                otherPos,
                state.setValue(
                        PART,
                        Part.RIGHT
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

            destroyOtherHalf(
                    level,
                    pos,
                    state
            );
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
            BlockState state
    ) {
        Direction facing =
                state.getValue(FACING);

        Direction right =
                facing.getClockWise();

        BlockPos otherPos;

        if (state.getValue(PART) == Part.LEFT) {

            otherPos =
                    pos.relative(right);

        } else {

            otherPos =
                    pos.relative(
                            right.getOpposite()
                    );
        }

        BlockState otherState =
                level.getBlockState(otherPos);

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
        return PIANO_SHAPE;
    }
}