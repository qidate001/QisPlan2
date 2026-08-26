package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GhostLeatherBoxBlockEntity
        extends BlockEntity {

    /**
     * 尚未分配区域。
     */
    public static final long UNASSIGNED = -1L;

    /**
     * 当前鬼皮箱绑定的划分区域 ID。
     */
    private long regionId = UNASSIGNED;

    public GhostLeatherBoxBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                QisPlan2.GHOST_LEATHER_BOX_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    public boolean hasRegion() {
        return regionId >= 0L;
    }

    public long getRegionId() {
        return regionId;
    }

    public void setRegionId(
            long regionId
    ) {
        this.regionId = regionId;
        setChanged();
    }

    /**
     * NeoForge 21.1：
     * saveAdditional 需要 HolderLookup.Provider。
     */
    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        if (hasRegion()) {
            tag.putLong(
                    "RegionId",
                    regionId
            );
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(
                tag,
                registries
        );

        regionId =
                tag.contains("RegionId")
                        ? tag.getLong("RegionId")
                        : UNASSIGNED;
    }

    public void saveRegionToItem(
            ItemStack stack
    ) {

        if (!hasRegion()) {
            return;
        }

        stack.set(
                QisPlan2.GHOST_LEATHER_BOX_REGION_ID,
                regionId
        );
    }

    public void loadRegionFromItem(
            ItemStack stack
    ) {

        Long regionId =
                stack.get(
                        QisPlan2.GHOST_LEATHER_BOX_REGION_ID
                );

        if (regionId != null
                && regionId >= 0L) {

            this.regionId =
                    regionId;

            setChanged();
        }
    }
}