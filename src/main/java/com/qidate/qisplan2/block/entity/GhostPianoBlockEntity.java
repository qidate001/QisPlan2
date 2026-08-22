package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.event.GhostPianoMusicHandler;
import com.qidate.qisplan2.block.GhostPianoBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GhostPianoBlockEntity extends BlockEntity {

    public GhostPianoBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                QisPlan2.GHOST_PIANO_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            GhostPianoBlockEntity blockEntity
    ) {
        if (level.isClientSide()) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        /*
         * 只让 LEFT 半边负责登记。
         *
         * RIGHT 半边也拥有 BlockEntity，
         * 但不会重复注册。
         */
        if (state.getValue(
                GhostPianoBlock.PART
        ) != GhostPianoBlock.Part.LEFT) {
            return;
        }

        /*
         * 登记到持久化数据。
         *
         * SavedData 自己会去重，
         * 所以这里每 tick 调用也是安全的。
         */
        GhostPianoMusicHandler.registerPiano(
                serverLevel,
                pos
        );
    }
}