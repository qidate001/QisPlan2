package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GhostDoorPlateBlockEntity extends BlockEntity {

    public static final int DEFAULT_NUMBER = 666;

    private int number = DEFAULT_NUMBER;

    public GhostDoorPlateBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlocks.GHOST_DOOR_PLATE_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {

        this.number = number;

        /*
         * 标记数据发生变化。
         */
        setChanged();

        /*
         * 如果已经在世界中，
         * 立即通知客户端更新这个 BlockEntity。
         */
        if (level != null) {

            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {

        CompoundTag tag =
                super.getUpdateTag(
                        registries
                );

        tag.putInt(
                "Number",
                number
        );

        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        super.saveAdditional(tag, provider);

        tag.putInt("Number", number);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        super.loadAdditional(tag, provider);

        if (tag.contains("Number")) {
            number = tag.getInt("Number");
        } else {
            number = DEFAULT_NUMBER;
        }
    }
}