package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.structure.GhostManorGenerationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GhostManorMarkerBlockEntity extends BlockEntity {

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

        QisPlan2.LOGGER.info(
                "[QisPlan2] Ghost Manor Marker tick: {}",
                pos
        );

        boolean started =
                GhostManorGenerationManager.start(
                        serverLevel,
                        pos
                );

        if (started) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2] Ghost Manor Marker 已接管：{}",
                    pos
            );

            serverLevel.removeBlock(
                    pos,
                    false
            );
        }
    }
}