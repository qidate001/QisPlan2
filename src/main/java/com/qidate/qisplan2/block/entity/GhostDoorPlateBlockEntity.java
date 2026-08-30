package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.ghost.doorplate.GhostDoorPlateRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GhostDoorPlateBlockEntity extends BlockEntity {

    public static final int DEFAULT_NUMBER = 666;

    private int number = DEFAULT_NUMBER;

    /*
     * ============================================================
     * 绑定鬼门
     * ============================================================
     */

    private BlockPos linkedDoorPos;

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

        setChanged();

        if (level instanceof ServerLevel serverLevel) {

            GhostDoorPlateRegistry.register(
                    number,
                    serverLevel,
                    worldPosition
            );
        }

        if (level != null) {

            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    public BlockPos getLinkedDoorPos() {
        return linkedDoorPos;
    }

    public void setLinkedDoorPos(
            BlockPos pos
    ) {

        linkedDoorPos =
                pos == null
                        ? null
                        : pos.immutable();

        setChanged();

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

        if (linkedDoorPos != null) {
            tag.putLong(
                    "LinkedDoorPos",
                    linkedDoorPos.asLong()
            );
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        super.loadAdditional(tag, provider);

        number =
                tag.contains("Number")
                        ? tag.getInt("Number")
                        : DEFAULT_NUMBER;

        if (tag.contains("LinkedDoorPos")) {

            linkedDoorPos =
                    BlockPos.of(
                            tag.getLong("LinkedDoorPos")
                    );

        } else {

            linkedDoorPos = null;
        }
    }

    @Override
    public void setLevel(
            net.minecraft.world.level.Level level
    ) {
        super.setLevel(level);

        if (level instanceof ServerLevel serverLevel) {

            GhostDoorPlateRegistry.register(
                    number,
                    serverLevel,
                    worldPosition
            );
        }
    }
}