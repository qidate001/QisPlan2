package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;
import com.qidate.qisplan2.block.entity.GhostTombstoneBlockEntity;
import com.qidate.qisplan2.core.ModTags;
import com.qidate.qisplan2.network.ghosttombstone.GhostTombstoneNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GhostTombstoneBlock
        extends HorizontalDirectionalBlock
        implements EntityBlock {

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

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new GhostTombstoneBlockEntity(
                pos,
                state
        );
    }

    public static final EnumProperty<Part> PART =
            EnumProperty.create(
                    "part",
                    Part.class
            );

    /*
     * 下半部分碰撞箱。
     *
     * 模型厚度：
     * Z = 2 ~ 4
     */
    private static final VoxelShape LOWER_NORTH_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    14.0 / 16.0,
                    1.0,
                    4.0 / 16.0
            );

    private static final VoxelShape LOWER_SOUTH_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    12.0 / 16.0,
                    14.0 / 16.0,
                    1.0,
                    14.0 / 16.0
            );

    private static final VoxelShape LOWER_WEST_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    4.0 / 16.0,
                    1.0,
                    14.0 / 16.0
            );

    private static final VoxelShape LOWER_EAST_SHAPE =
            Shapes.box(
                    12.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    14.0 / 16.0,
                    1.0,
                    14.0 / 16.0
            );

    /*
     * 上半部分碰撞箱。
     *
     * 模型主体：
     * X = 2 ~ 14
     * Y = 0 ~ 13
     * Z = 2 ~ 4
     */
    private static final VoxelShape UPPER_NORTH_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    14.0 / 16.0,
                    13.0 / 16.0,
                    4.0 / 16.0
            );

    private static final VoxelShape UPPER_SOUTH_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    12.0 / 16.0,
                    14.0 / 16.0,
                    13.0 / 16.0,
                    14.0 / 16.0
            );

    private static final VoxelShape UPPER_WEST_SHAPE =
            Shapes.box(
                    2.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    4.0 / 16.0,
                    13.0 / 16.0,
                    14.0 / 16.0
            );

    private static final VoxelShape UPPER_EAST_SHAPE =
            Shapes.box(
                    12.0 / 16.0,
                    0.0,
                    2.0 / 16.0,
                    14.0 / 16.0,
                    13.0 / 16.0,
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

    /**
     * 获取鬼墓碑数据实际存储的位置。
     *
     * 上半部分的 BlockEntity 不单独存数据，
     * 所有刻字统一存放在下半部分。
     */
    private static BlockPos getDataPos(
            BlockPos pos,
            BlockState state
    ) {

        if (state.getValue(PART)
                == Part.UPPER) {

            return pos.below();
        }

        return pos;
    }

    /**
     * 右键鬼墓碑。
     *
     * 只有手持可以刻字的灵异物品时，
     * 才会打开刻字界面。
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
         * ========================================================
         * 检查手中的物品
         * ========================================================
         */

        ItemStack stack =
                player.getMainHandItem();

        if (!stack.is(
                ModTags.Items.GHOST_TOMBSTONE_INSCRIBABLE
        )) {

            return InteractionResult.PASS;
        }


        /*
         * ========================================================
         * 客户端
         * ========================================================
         *
         * 客户端只告诉 Minecraft：
         * 这个右键交互已经被处理。
         *
         * 真正打开 GUI 由服务器发包。
         */

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }


        /*
         * ========================================================
         * 必须是服务器玩家
         * ========================================================
         */

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }


        /*
         * ========================================================
         * 获取真正保存数据的墓碑位置
         * ========================================================
         *
         * 上半部分没有单独保存刻字，
         * 所以需要找到下面的 BlockEntity。
         */

        BlockPos dataPos =
                getDataPos(
                        pos,
                        state
                );


        /*
         * ========================================================
         * 检查墓碑 BlockEntity
         * ========================================================
         */

        if (!(level.getBlockEntity(dataPos)
                instanceof GhostTombstoneBlockEntity)) {

            return InteractionResult.PASS;
        }


        /*
         * ========================================================
         * 打开刻字界面
         * ========================================================
         */

        GhostTombstoneNetwork.sendOpenScreen(
                serverPlayer,
                dataPos
        );

        return InteractionResult.SUCCESS;
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