package com.qidate.qisplan2.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public class VillageGhostData extends SavedData {

    private static final String DATA_NAME =
            "qisplan2_village_ghosts";

    private final Set<String> processedVillages =
            new HashSet<>();

    public VillageGhostData() {
    }

    /**
     * 从世界存档读取。
     */
    public VillageGhostData(CompoundTag tag) {

        ListTag list =
                tag.getList(
                        "ProcessedVillages",
                        Tag.TAG_STRING
                );

        for (int i = 0; i < list.size(); i++) {

            processedVillages.add(
                    list.getString(i)
            );
        }
    }

    /**
     * 判断村庄是否已经处理过。
     */
    public boolean isProcessed(String villageId) {
        return processedVillages.contains(villageId);
    }

    /**
     * 标记村庄已经处理。
     */
    public void markProcessed(String villageId) {

        if (processedVillages.add(villageId)) {
            setDirty();
        }
    }

    /**
     * 保存到世界存档。
     */
    @Override
    public CompoundTag save(
            CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries
    ) {

        ListTag list = new ListTag();

        for (String villageId : processedVillages) {

            list.add(
                    net.minecraft.nbt.StringTag.valueOf(
                            villageId
                    )
            );
        }

        tag.put(
                "ProcessedVillages",
                list
        );

        return tag;
    }

    /**
     * 获取当前世界的 VillageGhostData。
     */
    public static VillageGhostData get(ServerLevel level) {

        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        VillageGhostData::new,
                        (tag, registries) -> new VillageGhostData(tag),
                        null
                ),
                DATA_NAME
        );
    }
}