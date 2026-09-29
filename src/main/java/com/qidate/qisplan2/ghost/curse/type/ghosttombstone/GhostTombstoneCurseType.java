package com.qidate.qisplan2.ghost.curse.type.ghosttombstone;

import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseSource;
import com.qidate.qisplan2.ghost.curse.CurseType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 鬼墓碑诅咒类型。
 *
 * 负责：
 *
 * 1. 创建新的鬼墓碑诅咒
 * 2. 从存档恢复鬼墓碑诅咒
 */
public class GhostTombstoneCurseType
        implements CurseType {

    /**
     * 鬼墓碑诅咒类型 ID。
     */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "ghost_tombstone"
            );

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /**
     * 创建新的鬼墓碑诅咒。
     */
    @Override
    public Curse create(
            UUID target,
            CurseSource source,
            CompoundTag initialState
    ) {

        int strength =
                initialState.getInt("Strength");

        return new GhostTombstoneCurse(
                target,
                (GhostTombstoneCurseSource) source,
                strength
        );
    }

    @Override
    public ResourceLocation icon() {
        return null;
    }

    @Override
    public boolean canDetect() {
        return true;
    }

    /**
     * 从存档恢复鬼墓碑诅咒。
     *
     * CurseSavedData 不需要知道：
     *
     * - Id 怎么读取
     * - Target 怎么读取
     * - Source 怎么读取
     *
     * 全部由这里负责。
     */
    @Override
    public Curse load(
            CompoundTag tag
    ) {

        /*
         * 恢复诅咒实例 UUID。
         */
        UUID id =
                tag.getUUID("Id");

        /*
         * 恢复目标玩家 UUID。
         */
        UUID target =
                tag.getUUID("Target");

        /*
         * 恢复来源数据。
         */
        GhostTombstoneCurseSource source =
                GhostTombstoneCurseSource.load(
                        tag.getCompound("Source")
                );

        /*
         * 恢复强度状态数据。
         */
        int strength =
                tag.getInt("Strength");

        /*
         * 创建恢复后的诅咒实例。
         */
        return new GhostTombstoneCurse(
                id,
                target,
                source,
                strength
        );
    }
}