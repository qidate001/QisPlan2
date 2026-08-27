package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.structure.GhostLakeGenerationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GhostLakeMarkerBlockEntity
        extends BlockEntity {

    /**
     * 出生点禁区半径。
     *
     * 暂时与鬼庄园保持一致。
     */
    private static final double SPAWN_EXCLUSION_RADIUS =
            500.0D;

    public GhostLakeMarkerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                QisPlan2.GHOST_LAKE_MARKER_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            GhostLakeMarkerBlockEntity blockEntity
    ) {
        if (level.isClientSide()) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        /*
         * ========================================================
         * 出生点禁区
         * ========================================================
         */

        BlockPos spawnPos =
                serverLevel.getSharedSpawnPos();

        double distanceSqr =
                pos.distSqr(
                        spawnPos
                );

        double radiusSqr =
                SPAWN_EXCLUSION_RADIUS
                        * SPAWN_EXCLUSION_RADIUS;

        if (distanceSqr < radiusSqr) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 鬼湖候选点位于出生点禁区内，取消生成：{}",
                    pos
            );

            /*
             * 只删除 Marker。
             */
            serverLevel.removeBlock(
                    pos,
                    false
            );

            return;
        }

        /*
         * ========================================================
         * 正常启动鬼湖生成
         * ========================================================
         */

        boolean started =
                GhostLakeGenerationManager.start(
                        serverLevel,
                        pos
                );

        if (started) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 鬼湖 Marker 启动大型结构生成：{}",
                    pos
            );

            /*
             * 启动成功后删除 Marker。
             */
            serverLevel.removeBlock(
                    pos,
                    false
            );
        }
    }
}