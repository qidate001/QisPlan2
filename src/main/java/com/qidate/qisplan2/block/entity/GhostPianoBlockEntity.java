package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.GhostPianoBlock;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.event.GhostPianoMusicHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;

public class GhostPianoBlockEntity extends BlockEntity {

    public GhostPianoBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlocks.GHOST_PIANO_BLOCK_ENTITY.get(),
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
         * 只有左半边负责：
         *
         * 1. 注册钢琴
         * 2. 生成音符粒子
         */
        if (state.getValue(
                GhostPianoBlock.PART
        ) != GhostPianoBlock.Part.LEFT) {
            return;
        }

        /*
         * ========================================
         * 注册钢琴
         * ========================================
         */
        GhostPianoMusicHandler.registerPiano(
                serverLevel,
                pos
        );

        /*
         * ========================================
         * 鬼音乐符粒子
         * ========================================
         *
         * 平均大约每 5 tick 生成一次。
         */
        if (serverLevel.random.nextInt(5) != 0) {
            return;
        }

        /*
         * 钢琴占两个方块。
         *
         * 先找到右半边的位置。
         */
        net.minecraft.core.Direction facing =
                state.getValue(
                        GhostPianoBlock.FACING
                );

        net.minecraft.core.Direction right =
                facing.getClockWise();

        BlockPos rightPos =
                pos.relative(right);

        /*
         * 在两块钢琴上方随机生成一个音符。
         */
        double x =
                rightPos.getX()
                        + 0.5D;

        /*
         * 让 X/Z 在整个两方块区域里随机。
         */
        double offset =
                serverLevel.random.nextDouble();

        double x1 =
                Math.min(
                        pos.getX(),
                        rightPos.getX()
                );

        double x2 =
                Math.max(
                        pos.getX(),
                        rightPos.getX()
                );

        double z1 =
                Math.min(
                        pos.getZ(),
                        rightPos.getZ()
                );

        double z2 =
                Math.max(
                        pos.getZ(),
                        rightPos.getZ()
                );

        x =
                x1
                        + 0.2D
                        + (x2 - x1 + 1.0D)
                        * serverLevel.random.nextDouble();

        double z =
                z1
                        + 0.2D
                        + (z2 - z1 + 1.0D)
                        * serverLevel.random.nextDouble();

        double y =
                pos.getY()
                        + 1.4D
                        + serverLevel.random.nextDouble()
                        * 0.7D;

        /*
         * 生成音符粒子。
         */
        serverLevel.sendParticles(
                ParticleTypes.NOTE,
                x,
                y,
                z,
                1,
                0.0D,
                0.15D,
                0.0D,
                0.0D
        );
    }
}