package com.qidate.qisplan2.block;

import com.mojang.serialization.MapCodec;
import com.qidate.qisplan2.block.entity.GhostIsolationMarkerBlockEntity;
import com.qidate.qisplan2.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class GhostIsolationMarkerBlock
        extends BaseEntityBlock {

    public GhostIsolationMarkerBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(
                GhostIsolationMarkerBlock::new
        );
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new GhostIsolationMarkerBlockEntity(
                pos,
                state
        );
    }

    @Override
    public <T extends BlockEntity> @Nullable
            BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {

        /*
         * 客户端不执行任何检测逻辑。
         */
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(
                blockEntityType,
                ModBlocks.GHOST_ISOLATION_MARKER_BLOCK_ENTITY.get(),
                GhostIsolationMarkerBlockEntity::serverTick
        );
    }
}