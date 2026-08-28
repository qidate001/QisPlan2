package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.entity.GhostLeatherBoxBlockEntity;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.ghost.partition.PartitionReturnManager;
import com.qidate.qisplan2.ghost.partition.PartitionRoomPos;
import com.qidate.qisplan2.ghost.partition.PartitionSpaceManager;
import com.qidate.qisplan2.ghost.partition.PartitionSpaceSavedData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class GhostLeatherBoxBlock
        extends BaseEntityBlock {

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public static final MapCodec<GhostLeatherBoxBlock> CODEC =
            simpleCodec(
                    GhostLeatherBoxBlock::new
            );

    public GhostLeatherBoxBlock(
            Properties properties
    ) {
        super(
                properties
                        .sound(SoundType.WOOD)
        );

        registerDefaultState(
                stateDefinition.any().setValue(
                        FACING,
                        Direction.NORTH
                )
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {

        /*
         * 客户端只响应交互。
         */
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        /*
         * 必须是服务器玩家。
         */
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        /*
         * 获取箱子的 BlockEntity。
         */
        if (!(level.getBlockEntity(pos)
                instanceof GhostLeatherBoxBlockEntity box)) {

            return InteractionResult.PASS;
        }

        MinecraftServer server =
                serverPlayer.server;

        /*
         * ========================================================
         * 第一次使用：
         *
         * 分配一个永久区域 ID。
         * ========================================================
         */
        if (!box.hasRegion()) {

            PartitionSpaceSavedData data =
                    PartitionSpaceSavedData.get(
                            server
                    );

            long regionId =
                    data.allocateRegion();

            box.setRegionId(
                    regionId
            );

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 鬼皮箱首次分配区域：regionId={}",
                    regionId
            );
        }

        /*
         * ========================================================
         * 获取划分维度。
         * ========================================================
         */
        ServerLevel partitionLevel =
                server.getLevel(
                        QisPlan2.PARTITION_DIMENSION
                );

        if (partitionLevel == null) {

            QisPlan2.LOGGER.error(
                    "[QisPlan2] 找不到划分维度：{}",
                    QisPlan2.PARTITION_DIMENSION
            );

            return InteractionResult.FAIL;
        }

        long regionId =
                box.getRegionId();

        PartitionRoomPos initialRoom =
                new PartitionRoomPos(
                        0,
                        0,
                        0
                );

        BlockPos roomCenter =
                PartitionSpaceManager.getRoomCenter(
                        regionId,
                        initialRoom
                );

        double x =
                roomCenter.getX() + 0.5D;

        double y =
                roomCenter.getY();

        double z =
                roomCenter.getZ() + 0.5D;

        /*
         * ========================================================
         * 确保该区域已经生成初始石盒。
         * ========================================================
         */
        PartitionSpaceManager.ensureInitialRoom(
                partitionLevel,
                regionId
        );

        /*
         * ========================================================
         * 记录玩家原来的位置。
         *
         * 以后从出口方块出去，
         * 就回到这里。
         * ========================================================
         */
        PartitionReturnManager.capture(
                serverPlayer
        );

        /*
         * ========================================================
         * 传送。
         * ========================================================
         */
        DimensionTransition transition =
                new DimensionTransition(
                        partitionLevel,
                        new Vec3(
                                x,
                                y,
                                z
                        ),
                        serverPlayer.getDeltaMovement(),
                        serverPlayer.getYRot(),
                        serverPlayer.getXRot(),
                        DimensionTransition.DO_NOTHING
                );

        serverPlayer.changeDimension(
                transition
        );

        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {

        return new GhostLeatherBoxBlockEntity(
                pos,
                state
        );
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {

        return RenderShape.MODEL;
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

        if (level.getBlockEntity(pos)
                instanceof GhostLeatherBoxBlockEntity box) {

            box.loadRegionFromItem(
                    stack
            );
        }
    }

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            ItemStack tool
    ) {

        if (level.isClientSide()) {
            return;
        }

        /*
         * ========================================================
         * 创建掉落物
         * ========================================================
         */

        ItemStack drop =
                new ItemStack(
                        ModBlocks.GHOST_LEATHER_BOX_ITEM.get()
                );

        /*
         * ========================================================
         * 携带 RegionId
         * ========================================================
         */

        if (blockEntity
                instanceof GhostLeatherBoxBlockEntity box) {

            box.saveRegionToItem(
                    drop
            );
        }

        /*
         * ========================================================
         * 掉落
         * ========================================================
         */

        popResource(
                level,
                pos,
                drop
        );

        /*
         * 不调用 super.playerDestroy()
         *
         * 防止原版再生成一次普通鬼皮箱掉落。
         */
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {

        Long regionId =
                stack.get(
                        ModDataComponents.GHOST_LEATHER_BOX_REGION_ID
                );

        if (regionId != null) {

            tooltip.add(
                    Component.literal(
                            "区域 ID："
                                    + regionId
                    )
            );
        }

        super.appendHoverText(
                stack,
                context,
                tooltip,
                flag
        );
    }

    /**
     * 放置时根据玩家方向决定鬼皮箱朝向。
     */
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {

        return defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection()
                        .getOpposite()
        );
    }

    /**
     * 支持方块旋转。
     */
    @Override
    protected BlockState rotate(
            BlockState state,
            Rotation rotation
    ) {

        return state.setValue(
                FACING,
                rotation.rotate(
                        state.getValue(
                                FACING
                        )
                )
        );
    }

    /**
     * 支持方块镜像。
     */
    @Override
    protected BlockState mirror(
            BlockState state,
            Mirror mirror
    ) {

        return state.rotate(
                mirror.getRotation(
                        state.getValue(
                                FACING
                        )
                )
        );
    }

    /**
     * 注册 FACING 属性。
     */
    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<
                    Block,
                    BlockState
                    > builder
    ) {

        builder.add(
                FACING
        );
    }
}