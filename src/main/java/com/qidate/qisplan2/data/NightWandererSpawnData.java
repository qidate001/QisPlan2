package com.qidate.qisplan2.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;

public class NightWandererSpawnData extends SavedData {

    private static final String HAS_SPAWNED =
            "QisPlan2NightWandererSpawned";

    private boolean spawned = false;

    public static NightWandererSpawnData create() {
        return new NightWandererSpawnData();
    }

    public static NightWandererSpawnData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        NightWandererSpawnData data =
                new NightWandererSpawnData();

        data.spawned =
                tag.getBoolean(HAS_SPAWNED);

        return data;
    }

    public boolean hasSpawned() {
        return spawned;
    }

    public void markSpawned() {
        if (spawned) {
            return;
        }

        spawned = true;
        setDirty();
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        tag.putBoolean(
                HAS_SPAWNED,
                spawned
        );

        return tag;
    }
}