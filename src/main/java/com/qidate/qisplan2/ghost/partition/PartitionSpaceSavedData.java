package com.qidate.qisplan2.ghost.partition;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PartitionSpaceSavedData
        extends SavedData {

    private static final String DATA_NAME =
            "qisplan2_partition_spaces";

    private long nextRegionId = 0L;

    /**
     * 已经创建过的区域。
     */
    private final Set<Long> initializedRegions =
            new HashSet<>();

    /**
     * 每个区域当前拥有的房间。
     */
    private final Map<
            Long,
            Set<PartitionRoomPos>
            > roomsByRegion =
            new HashMap<>();

    public PartitionSpaceSavedData() {
    }

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

        /*
         * ========================================================
         * 已初始化区域
         * ========================================================
         */

        long[] regions =
                tag.getLongArray(
                        "initialized_regions"
                );

        for (long regionId : regions) {

            if (regionId >= 0L) {

                data.initializedRegions.add(
                        regionId
                );
            }
        }

        /*
         * ========================================================
         * 房间
         * ========================================================
         */

        if (tag.contains(
                "rooms",
                Tag.TAG_LIST
        )) {

            ListTag roomList =
                    tag.getList(
                            "rooms",
                            Tag.TAG_COMPOUND
                    );

            for (Tag element :
                    roomList) {

                CompoundTag roomTag =
                        (CompoundTag) element;

                long regionId =
                        roomTag.getLong(
                                "region_id"
                        );

                PartitionRoomPos room =
                        new PartitionRoomPos(
                                roomTag.getInt("x"),
                                roomTag.getInt("y"),
                                roomTag.getInt("z")
                        );

                data.roomsByRegion
                        .computeIfAbsent(
                                regionId,
                                ignored ->
                                        new HashSet<>()
                        )
                        .add(room);
            }
        }

        return data;
    }

    /**
     * 分配新区域。
     */
    public long allocateRegion() {

        long id =
                nextRegionId++;

        initializedRegions.add(
                id
        );

        roomsByRegion.put(
                id,
                new HashSet<>()
        );

        setDirty();

        return id;
    }

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

    /**
     * 检查某个房间是否存在。
     */
    public boolean hasRoom(
            long regionId,
            PartitionRoomPos room
    ) {

        return roomsByRegion
                .getOrDefault(
                        regionId,
                        Set.of()
                )
                .contains(room);
    }

    /**
     * 创建房间记录。
     */
    public boolean addRoom(
            long regionId,
            PartitionRoomPos room
    ) {

        boolean added =
                roomsByRegion
                        .computeIfAbsent(
                                regionId,
                                ignored ->
                                        new HashSet<>()
                        )
                        .add(room);

        if (added) {
            setDirty();
        }

        return added;
    }

    public Set<PartitionRoomPos> getRooms(
            long regionId
    ) {

        return roomsByRegion.getOrDefault(
                regionId,
                Set.of()
        );
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {

        tag.putLong(
                "next_region_id",
                nextRegionId
        );

        /*
         * 已初始化区域。
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

        /*
         * 房间。
         */
        ListTag roomList =
                new ListTag();

        for (Map.Entry<
                Long,
                Set<PartitionRoomPos>
                > entry :
                roomsByRegion.entrySet()) {

            long regionId =
                    entry.getKey();

            for (PartitionRoomPos room :
                    entry.getValue()) {

                CompoundTag roomTag =
                        new CompoundTag();

                roomTag.putLong(
                        "region_id",
                        regionId
                );

                roomTag.putInt(
                        "x",
                        room.x()
                );

                roomTag.putInt(
                        "y",
                        room.y()
                );

                roomTag.putInt(
                        "z",
                        room.z()
                );

                roomList.add(
                        roomTag
                );
            }
        }

        tag.put(
                "rooms",
                roomList
        );

        return tag;
    }

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