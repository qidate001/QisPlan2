package com.qidate.qisplan2.ghost.partition;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class PartitionSpaceSavedData
        extends SavedData {

    private static final String DATA_NAME =
            "qisplan2_partition_spaces";

    /**
     * 下一次分配的区域 ID。
     */
    private long nextRegionId = 0L;

    public PartitionSpaceSavedData() {
    }

    /**
     * 从存档读取。
     */
    public static PartitionSpaceSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {

        PartitionSpaceSavedData data =
                new PartitionSpaceSavedData();

        data.nextRegionId =
                Math.max(
                        0L,
                        tag.getLong(
                                "next_region_id"
                        )
                );

        return data;
    }

    /**
     * 分配一个新的区域。
     */
    public long allocateRegion() {

        long id =
                nextRegionId++;

        setDirty();

        return id;
    }

    /**
     * NeoForge 21.1：
     * save 需要 HolderLookup.Provider。
     */
    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {

        tag.putLong(
                "next_region_id",
                nextRegionId
        );

        return tag;
    }

    /**
     * 获取全服务器共用的划分空间数据。
     *
     * 这类数据与具体维度无关，
     * 所以存到 Overworld。
     */
    public static PartitionSpaceSavedData get(
            MinecraftServer server
    ) {

        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                PartitionSpaceSavedData::new,
                                PartitionSpaceSavedData::load,
                                null
                        ),
                        DATA_NAME
                );
    }
}