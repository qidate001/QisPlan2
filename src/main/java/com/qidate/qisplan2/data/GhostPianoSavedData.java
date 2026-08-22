package com.qidate.qisplan2.data;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * 持久保存所有鬼钢琴的位置。
 *
 * 每个维度单独保存一份 SavedData。
 */
public class GhostPianoSavedData extends SavedData {

    private static final String DATA_NAME =
            "qisplan2_ghost_pianos";

    private static final String PIANOS_TAG =
            "pianos";

    private static final String POS_TAG =
            "pos";

    private final Set<Long> pianoPositions =
            new HashSet<>();

    public static GhostPianoSavedData create() {
        return new GhostPianoSavedData();
    }

    public static GhostPianoSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        GhostPianoSavedData data =
                new GhostPianoSavedData();

        ListTag list =
                tag.getList(
                        PIANOS_TAG,
                        Tag.TAG_LONG
                );

        for (int i = 0; i < list.size(); i++) {
            data.pianoPositions.add(
                    ((LongTag) list.get(i)).getAsLong()
            );
        }

        return data;
    }

    /**
     * 获取某个维度的 SavedData。
     */
    public static GhostPianoSavedData get(
            ServerLevel level
    ) {
        return level.getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                GhostPianoSavedData::create,
                                GhostPianoSavedData::load,
                                null
                        ),
                        DATA_NAME
                );
    }

    /**
     * 注册一架钢琴。
     */
    public boolean add(
            BlockPos pos
    ) {
        boolean changed =
                pianoPositions.add(
                        pos.asLong()
                );

        if (changed) {
            setDirty();
        }

        return changed;
    }

    /**
     * 注销一架钢琴。
     */
    public boolean remove(
            BlockPos pos
    ) {
        boolean changed =
                pianoPositions.remove(
                        pos.asLong()
                );

        if (changed) {
            setDirty();
        }

        return changed;
    }

    /**
     * 获取所有钢琴位置。
     */
    public Set<BlockPos> getPositions() {
        Set<BlockPos> result =
                new HashSet<>();

        for (long packed :
                pianoPositions) {

            result.add(
                    BlockPos.of(packed)
            );
        }

        return result;
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries
    ) {
        ListTag list =
                new ListTag();

        for (long packed :
                pianoPositions) {

            list.add(
                    net.minecraft.nbt.LongTag.valueOf(
                            packed
                    )
            );
        }

        tag.put(
                PIANOS_TAG,
                list
        );

        return tag;
    }
}