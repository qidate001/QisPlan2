package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;
import com.qidate.qisplan2.block.entity.GhostDoorPlateBlockEntity;
import com.qidate.qisplan2.network.QisNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class GhostDoorPlateBlock
        extends HorizontalDirectionalBlock
        implements EntityBlock
{

    /**
     * 鬼门牌朝向。
     */
    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;


    /**
     * 鬼门牌的选择框。
     *
     * 与 Blockbench 模型完全对应：
     *
     * X = 2 ~ 14
     * Y = 7 ~ 14
     * Z = 15 ~ 16
     *
     * 默认朝 NORTH。
     */
    private static final VoxelShape NORTH_SHAPE =
            Block.box(
                    2,
                    7,
                    15,
                    14,
                    14,
                    16
            );

    private static final VoxelShape SOUTH_SHAPE =
            Block.box(
                    2,
                    7,
                    0,
                    14,
                    14,
                    1
            );

    private static final VoxelShape WEST_SHAPE =
            Block.box(
                    15,
                    7,
                    2,
                    16,
                    14,
                    14
            );

    private static final VoxelShape EAST_SHAPE =
            Block.box(
                    0,
                    7,
                    2,
                    1,
                    14,
                    14
            );


    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new GhostDoorPlateBlockEntity(
                pos,
                state
        );
    }

    public static final MapCodec<GhostDoorPlateBlock> CODEC =
            simpleCodec(GhostDoorPlateBlock::new);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public GhostDoorPlateBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);

        /*
         * 默认朝北。
         */
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(
                                FACING,
                                Direction.NORTH
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
        builder.add(FACING);
    }


    /**
     * 放置鬼门牌时确定朝向。
     */
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }


    /**
     * 判断鬼门牌是否有墙体支撑。
     */
    @Override
    public boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {

        Direction facing =
                state.getValue(FACING);

        BlockPos supportPos =
                pos.relative(
                        facing.getOpposite()
                );

        return !level.getBlockState(
                supportPos
        ).isAir();
    }


    /**
     * 邻居发生变化时检查支撑。
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

        if (!canSurvive(
                state,
                level,
                pos
        )) {

            return Blocks.AIR.defaultBlockState();
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
     * 鬼门牌的选择框 / 碰撞箱。
     */
    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {

        return switch (
                state.getValue(FACING)
                ) {

            case NORTH -> NORTH_SHAPE;

            case SOUTH -> SOUTH_SHAPE;

            case WEST -> WEST_SHAPE;

            case EAST -> EAST_SHAPE;

            default -> NORTH_SHAPE;
        };
    }

    /**
     * 鬼门牌没有碰撞箱。
     */
    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.empty();
    }


    /**
     * 右键打开编辑GUI。
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
         * 客户端：
         *
         * 直接告诉 Minecraft：
         * 这个交互被处理了。
         *
         * 真正打开 GUI 由服务器发包。
         */
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        /*
         * 必须是玩家。
         */
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        /*
         * 必须真的存在鬼门牌 BlockEntity。
         */
        if (!(level.getBlockEntity(pos)
                instanceof GhostDoorPlateBlockEntity)) {

            return InteractionResult.PASS;
        }

        /*
         * 打开客户端编辑界面。
         */
        QisNetwork.sendOpenGhostDoorPlateScreen(
                serverPlayer,
                pos
        );

        return InteractionResult.SUCCESS;
    }
}