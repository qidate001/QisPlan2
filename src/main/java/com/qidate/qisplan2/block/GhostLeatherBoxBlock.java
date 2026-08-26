package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.entity.GhostLeatherBoxBlockEntity;
import com.qidate.qisplan2.ghost.partition.PartitionSpaceManager;
import com.qidate.qisplan2.ghost.partition.PartitionSpaceSavedData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GhostLeatherBoxBlock
        extends BaseEntityBlock {

    /**
     * 1.21.1 BaseEntityBlock 必须提供 codec。
     */
    public static final MapCodec<GhostLeatherBoxBlock> CODEC =
            simpleCodec(
                    GhostLeatherBoxBlock::new
            );

    public GhostLeatherBoxBlock(
            Properties properties
    ) {
        super(
                properties
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

        double x =
                PartitionSpaceManager.getCenterX(
                        regionId
                );

        double y =
                PartitionSpaceManager.getCenterY();

        double z =
                PartitionSpaceManager.getCenterZ(
                        regionId
                );

        /*
         * ========================================================
         * 1.21.1 DimensionTransition：
         *
         * 位置使用 Vec3
         * 速度使用 Vec3
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
}