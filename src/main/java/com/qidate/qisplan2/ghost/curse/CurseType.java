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
     * 诅咒类型 ID。
     */
    ResourceLocation id();

    /**
     * 获取该诅咒在 HUD 中使用的图标。
     */
    ResourceLocation icon();

    /**
     * 目标自身是否能够察觉这个诅咒。
     *
     * <p>
     * 当前阶段直接使用固定的 true / false。
     * 后续如果需要复杂的统一判断机制，
     * 再在这里扩展。
     * </p>
     */
    boolean canDetect();

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
     * 从保存的数据中恢复诅咒实例。
     */
    Curse load(CompoundTag tag);
}