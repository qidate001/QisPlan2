package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;
import com.qidate.qisplan2.ghost.partition.PartitionReturnManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

public class PartitionExitBlock
        extends Block {

    public static final MapCodec<PartitionExitBlock> CODEC =
            simpleCodec(
                    PartitionExitBlock::new
            );

    public PartitionExitBlock(
            Properties properties
    ) {
        super(
                properties
        );
    }

    @Override
    public MapCodec<? extends PartitionExitBlock> codec() {
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

        if (!(player instanceof ServerPlayer serverPlayer)) {

            return InteractionResult.PASS;
        }

        /*
         * 必须存在返回点。
         */
        if (!PartitionReturnManager.hasReturnPoint(
                serverPlayer
        )) {

            return InteractionResult.FAIL;
        }

        /*
         * 返回进入前的位置。
         */
        boolean success =
                PartitionReturnManager.returnPlayer(
                        serverPlayer
                );

        return success
                ? InteractionResult.CONSUME
                : InteractionResult.FAIL;
    }
}