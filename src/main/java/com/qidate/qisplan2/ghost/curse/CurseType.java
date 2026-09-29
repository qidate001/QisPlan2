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
 */
public interface CurseType {

    /**
     * 获取诅咒类型 ID。
     */
    ResourceLocation id();

    /**
     * 创建一个新的诅咒实例。
     *
     * @param target 诅咒目标
     * @param source 诅咒来源
     * @param initialState 创建时的初始状态
     */
    Curse create(
            UUID target,
            CurseSource source,
            CompoundTag initialState
    );

    /**
     * 从存档数据恢复一个诅咒实例。
     */
    Curse load(
            CompoundTag tag
    );
}