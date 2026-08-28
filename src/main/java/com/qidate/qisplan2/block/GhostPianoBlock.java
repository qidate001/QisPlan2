package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.entity.GhostPianoBlockEntity;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.event.GhostPianoMusicHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

public class GhostPianoBlock extends BaseEntityBlock {

    public static final MapCodec<GhostPianoBlock> CODEC =
            simpleCodec(GhostPianoBlock::new);

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

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

    private static final VoxelShape SHAPE =
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
    protected MapCodec<? extends BaseEntityBlock> codec() {
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
        Direction facing =
                context.getHorizontalDirection();

        Direction right =
                facing.getClockWise();

        BlockPos otherPos =
                context.getClickedPos()
                        .relative(right);

        /*
         * 第二个方块放不下，
         * 整个钢琴就不允许放置。
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
         * 放置 RIGHT。
         */
        level.setBlock(
                otherPos,
                state.setValue(
                        PART,
                        Part.RIGHT
                ),
                3
        );

        /*
         * 不在这里注册钢琴。
         *
         * 注册交给 BlockEntity ticker。
         */
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide()
                && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {

            if (state.getValue(PART)
                    == Part.LEFT) {

                GhostPianoMusicHandler.unregisterPiano(
                        serverLevel,
                        pos
                );

                destroyOtherHalf(
                        level,
                        pos,
                        state
                );

            } else {

                Direction right =
                        state.getValue(FACING)
                                .getClockWise();

                BlockPos leftPos =
                        pos.relative(
                                right.getOpposite()
                        );

                GhostPianoMusicHandler.unregisterPiano(
                        serverLevel,
                        leftPos
                );

                BlockState leftState =
                        level.getBlockState(
                                leftPos
                        );

                if (leftState.is(this)) {
                    level.destroyBlock(
                            leftPos,
                            false
                    );
                }
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
            BlockState state
    ) {
        Direction right =
                state.getValue(FACING)
                        .getClockWise();

        BlockPos otherPos =
                pos.relative(right);

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
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new GhostPianoBlockEntity(
                pos,
                state
        );
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide()) {
            return null;
        }

        if (state.getValue(PART)
                != Part.LEFT) {
            return null;
        }

        return createTickerHelper(
                blockEntityType,
                ModBlocks.GHOST_PIANO_BLOCK_ENTITY.get(),
                GhostPianoBlockEntity::serverTick
        );
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public float getDestroyProgress(
            BlockState state,
            Player player,
            BlockGetter level,
            BlockPos pos
    ) {
        if (!player.isCreative()) {
            return 0.0F;
        }

        return super.getDestroyProgress(
                state,
                player,
                level,
                pos
        );
    }
}