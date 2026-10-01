package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;
import com.qidate.qisplan2.block.entity.GhostGraveyardMarkerBlockEntity;
import com.qidate.qisplan2.core.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public class GhostGraveyardMarkerBlock
        extends BaseEntityBlock {

    public static final MapCodec<GhostGraveyardMarkerBlock> CODEC =
            simpleCodec(
                    GhostGraveyardMarkerBlock::new
            );

    public GhostGraveyardMarkerBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {

        return new GhostGraveyardMarkerBlockEntity(
                pos,
                state
        );
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {

        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(
                blockEntityType,
                ModBlocks.GHOST_GRAVEYARD_MARKER_BLOCK_ENTITY.get(),
                GhostGraveyardMarkerBlockEntity::serverTick
        );
    }
}