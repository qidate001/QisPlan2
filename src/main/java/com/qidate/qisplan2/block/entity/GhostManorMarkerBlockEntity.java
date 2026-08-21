package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.structure.GhostManorGenerationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GhostManorMarkerBlockEntity extends BlockEntity {
    private static final double SPAWN_EXCLUSION_RADIUS = 500.0D;

    public GhostManorMarkerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                QisPlan2.GHOST_MANOR_MARKER_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            GhostManorMarkerBlockEntity blockEntity
    ) {
        if (level.isClientSide()) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        /*
         * ========================================
         * 出生点禁区
         * ========================================
         */
        BlockPos spawnPos =
                serverLevel.getSharedSpawnPos();

        double distanceSqr =
                pos.distSqr(spawnPos);

        double radiusSqr =
                SPAWN_EXCLUSION_RADIUS
                        * SPAWN_EXCLUSION_RADIUS;

        if (distanceSqr < radiusSqr) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 鬼庄园候选点位于出生点禁区内，取消生成：{}",
                    pos
            );

            /*
             * 只删除 Marker，
             * 不启动大型结构生成。
             */
            serverLevel.removeBlock(
                    pos,
                    false
            );

            return;
        }

        /*
         * ========================================
         * 正常生成
         * ========================================
         */
        boolean started =
                GhostManorGenerationManager.start(
                        serverLevel,
                        pos
                );

        if (started) {

            serverLevel.removeBlock(
                    pos,
                    false
            );
        }
    }
}