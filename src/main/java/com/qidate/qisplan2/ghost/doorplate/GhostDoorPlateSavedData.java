package com.qidate.qisplan2.ghost.doorplate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * 鬼门牌持久化数据。
 *
 * 整个服务器共用一份。
 *
 * 数据保存于主世界的 SavedData 中，
 * 因此可以跨维度记录鬼门牌。
 *
 * 门牌号 -> 所有对应门牌的位置。
 */
public class GhostDoorPlateSavedData
        extends SavedData {

    /**
     * SavedData 文件名称。
     */
    public static final String DATA_NAME =
            "qisplan2_ghost_door_plates";


    /**
     * 门牌注册表。
     *
     * 门牌号 -> 门牌位置集合
     */
    private final Map<
            Integer,
            Set<GhostDoorPlateRegistry.DoorLocation>
            > plates =
            new HashMap<>();


    /*
     * ============================================================
     * SavedData Factory
     * ============================================================
     */

    public static final Factory<GhostDoorPlateSavedData> FACTORY =
            new Factory<>(
                    GhostDoorPlateSavedData::new,
                    GhostDoorPlateSavedData::load,
                    null
            );


    /*
     * ============================================================
     * 构造
     * ============================================================
     */

    public GhostDoorPlateSavedData() {
    }


    /*
     * ============================================================
     * 获取
     * ============================================================
     */

    public static GhostDoorPlateSavedData get(
            ServerLevel level
    ) {

        /*
         * 所有维度统一使用主世界的 SavedData。
         *
         * 这样：
         *
         * Overworld
         * Nether
         * End
         *
         * 都访问同一份鬼门牌数据。
         */
        ServerLevel overworld =
                level.getServer().overworld();

        return overworld
                .getDataStorage()
                .computeIfAbsent(
                        FACTORY,
                        DATA_NAME
                );
    }


    /*
     * ============================================================
     * 注册
     * ============================================================
     */

    public void register(
            int number,
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {

        /*
         * 同一个位置不能同时属于两个门牌号。
         */
        unregisterPosition(
                dimension,
                pos
        );

        plates
                .computeIfAbsent(
                        number,
                        ignored -> new HashSet<>()
                )
                .add(
                        new GhostDoorPlateRegistry.DoorLocation(
                                dimension,
                                pos
                        )
                );

        setDirty();
    }


    /*
     * ============================================================
     * 删除指定位置
     * ============================================================
     */

    public void unregisterPosition(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {

        boolean changed = false;

        Iterator<
                Map.Entry<
                        Integer,
                        Set<GhostDoorPlateRegistry.DoorLocation>
                        >
                > iterator =
                plates.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<
                    Integer,
                    Set<GhostDoorPlateRegistry.DoorLocation>
                    > entry =
                    iterator.next();

            Set<
                    GhostDoorPlateRegistry.DoorLocation
                    > locations =
                    entry.getValue();

            boolean removed =
                    locations.removeIf(
                            location ->
                                    location.dimension()
                                            .equals(dimension)
                                            &&
                                            location.pos()
                                                    .equals(pos)
                    );

            if (removed) {
                changed = true;
            }

            if (locations.isEmpty()) {
                iterator.remove();
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }
    }


    /*
     * ============================================================
     * 获取某个门牌号
     * ============================================================
     */

    public Set<GhostDoorPlateRegistry.DoorLocation> getLocations(
            int number
    ) {

        Set<
                GhostDoorPlateRegistry.DoorLocation
                > locations =
                plates.get(number);

        if (locations == null) {
            return Set.of();
        }

        return Collections.unmodifiableSet(
                locations
        );
    }


    /*
     * ============================================================
     * 获取全部数据
     * ============================================================
     */

    public Map<
            Integer,
            Set<GhostDoorPlateRegistry.DoorLocation>
            > getAll() {

        return Collections.unmodifiableMap(
                plates
        );
    }


    /*
     * ============================================================
     * 保存
     * ============================================================
     */

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {

        ListTag numbers =
                new ListTag();

        for (
                Map.Entry<
                        Integer,
                        Set<GhostDoorPlateRegistry.DoorLocation>
                        > entry
                : plates.entrySet()
        ) {

            CompoundTag numberTag =
                    new CompoundTag();

            numberTag.putInt(
                    "Number",
                    entry.getKey()
            );

            ListTag locations =
                    new ListTag();

            for (
                    GhostDoorPlateRegistry.DoorLocation location
                    : entry.getValue()
            ) {

                CompoundTag locationTag =
                        new CompoundTag();

                locationTag.putString(
                        "Dimension",
                        location.dimension()
                                .location()
                                .toString()
                );

                locationTag.putLong(
                        "Pos",
                        location.pos()
                                .asLong()
                );

                locations.add(
                        locationTag
                );
            }

            numberTag.put(
                    "Locations",
                    locations
            );

            numbers.add(
                    numberTag
            );
        }

        tag.put(
                "Plates",
                numbers
        );

        return tag;
    }


    /*
     * ============================================================
     * 加载
     * ============================================================
     */

    public static GhostDoorPlateSavedData load(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {

        GhostDoorPlateSavedData data =
                new GhostDoorPlateSavedData();

        ListTag numbers =
                tag.getList(
                        "Plates",
                        CompoundTag.TAG_COMPOUND
                );

        for (int i = 0; i < numbers.size(); i++) {

            CompoundTag numberTag =
                    numbers.getCompound(i);

            int number =
                    numberTag.getInt("Number");

            ListTag locations =
                    numberTag.getList(
                            "Locations",
                            CompoundTag.TAG_COMPOUND
                    );

            Set<
                    GhostDoorPlateRegistry.DoorLocation
                    > locationSet =
                    data.plates.computeIfAbsent(
                            number,
                            ignored -> new HashSet<>()
                    );

            for (
                    int j = 0;
                    j < locations.size();
                    j++
            ) {

                CompoundTag locationTag =
                        locations.getCompound(j);

                String dimensionString =
                        locationTag.getString(
                                "Dimension"
                        );

                ResourceLocation dimensionId =
                        ResourceLocation.parse(
                                dimensionString
                        );

                ResourceKey<Level> dimension =
                        ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION,
                                dimensionId
                        );

                BlockPos pos =
                        BlockPos.of(
                                locationTag.getLong(
                                        "Pos"
                                )
                        );

                locationSet.add(
                        new GhostDoorPlateRegistry.DoorLocation(
                                dimension,
                                pos
                        )
                );
            }
        }

        return data;
    }
}