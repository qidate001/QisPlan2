package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GhostTombstoneBlockEntity extends BlockEntity {

    /**
     * 当前墓碑上刻写的文字。
     */
    private String inscription = "";

    public GhostTombstoneBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlocks.GHOST_TOMBSTONE_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    public String getInscription() {
        return inscription;
    }

    /**
     * 设置墓碑上的刻字。
     */
    public void setInscription(
            String inscription
    ) {

        this.inscription =
                inscription == null
                        ? ""
                        : inscription;

        setChanged();

        /*
         * 通知客户端更新 BlockEntity。
         */
        if (level instanceof ServerLevel) {

            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {

        super.saveAdditional(
                tag,
                provider
        );

        tag.putString(
                "Inscription",
                inscription
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {

        super.loadAdditional(
                tag,
                provider
        );

        inscription =
                tag.contains("Inscription")
                        ? tag.getString("Inscription")
                        : "";
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {

        CompoundTag tag =
                super.getUpdateTag(
                        registries
                );

        tag.putString(
                "Inscription",
                inscription
        );

        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }
}