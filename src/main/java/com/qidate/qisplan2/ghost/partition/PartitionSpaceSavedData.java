package com.qidate.qisplan2.ghost.partition;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public class PartitionSpaceSavedData
        extends SavedData {

    private static final String DATA_NAME =
            "qisplan2_partition_spaces";

    /*
     * ============================================================
     * 区域 ID 分配
     * ============================================================
     */

    /**
     * 下一个可分配区域 ID。
     */
    private long nextRegionId = 0L;


    /*
     * ============================================================
     * 区域初始化记录
     * ============================================================
     */

    /**
     * 已经完成初始空间生成的区域。
     *
     * 例如：
     *
     * 0 → 已生成
     * 1 → 已生成
     */
    private final Set<Long> initializedRegions =
            new HashSet<>();


    public PartitionSpaceSavedData() {
    }


    /*
     * ============================================================
     * 加载
     * ============================================================
     */

    public static PartitionSpaceSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {

        PartitionSpaceSavedData data =
                new PartitionSpaceSavedData();

        /*
         * 下一 ID。
         */
        data.nextRegionId =
                Math.max(
                        0L,
                        tag.getLong(
                                "next_region_id"
                        )
                );


        /*
         * 已初始化区域。
         */
        long[] regions =
                tag.getLongArray(
                        "initialized_regions"
                );

        for (long regionId :
                regions) {

            if (regionId >= 0L) {

                data.initializedRegions.add(
                        regionId
                );
            }
        }

        return data;
    }


    /*
     * ============================================================
     * 区域分配
     * ============================================================
     */

    public long allocateRegion() {

        long id =
                nextRegionId++;

        setDirty();

        return id;
    }


    /*
     * ============================================================
     * 初始化状态
     * ============================================================
     */

    public boolean isRegionInitialized(
            long regionId
    ) {

        return initializedRegions.contains(
                regionId
        );
    }


    public void markRegionInitialized(
            long regionId
    ) {

        if (initializedRegions.add(
                regionId
        )) {

            setDirty();
        }
    }


    /*
     * ============================================================
     * 保存
     * ============================================================
     */

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {

        /*
         * 下一 ID。
         */
        tag.putLong(
                "next_region_id",
                nextRegionId
        );


        /*
         * 初始化区域。
         */
        long[] regions =
                new long[
                        initializedRegions.size()
                        ];

        int index = 0;

        for (long regionId :
                initializedRegions) {

            regions[index++] =
                    regionId;
        }

        tag.putLongArray(
                "initialized_regions",
                regions
        );


        return tag;
    }


    /*
     * ============================================================
     * 获取全服务器数据
     * ============================================================
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