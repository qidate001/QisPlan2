package com.qidate.qisplan2.ghost.curse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 一个诅咒类型。
 *
 * CurseType 不是一个具体诅咒实例。
 *
 * 它代表：
 *
 * “这种诅咒应该如何创建，以及应该如何从存档恢复。”
 *
 * 例如：
 *
 * GhostTombstoneCurseType
 *
 * 就负责：
 *
 * qisplan2:ghost_tombstone
 *
 * 这种诅咒的创建与恢复。
 */
public interface CurseType {

    /**
     * 获取诅咒类型 ID。
     *
     * 例如：
     *
     * qisplan2:ghost_tombstone
     */
    ResourceLocation id();

    /**
     * 创建一个全新的诅咒实例。
     *
     * 此时诅咒是第一次产生，
     * 因此具体 Curse 自己负责生成新的实例 UUID。
     */
    Curse create(
            UUID target,
            CurseSource source
    );

    /**
     * 从存档数据恢复一个诅咒实例。
     *
     * 这里传入的是这个 Curse 保存下来的完整 NBT。
     *
     * CurseType 自己知道：
     *
     * - Id 在哪里
     * - Target 在哪里
     * - Source 如何恢复
     * - 自己还有哪些特殊数据
     *
     * 因此 CurseSavedData 不需要理解这些内容。
     */
    Curse load(
            CompoundTag tag
    );
}