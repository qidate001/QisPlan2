package com.qidate.qisplan2.ghost.curse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * 一个诅咒的来源。
 *
 * CurseSource 只负责描述：
 *
 * “这个诅咒是从哪里来的？”
 *
 * 例如：
 *
 * 鬼墓碑
 * 鬼实体
 * 鬼物品
 * 鬼域
 * 特殊设施
 * 等等。
 *
 * CurseSource 本身不负责：
 *
 * - 创建 Curse
 * - Tick Curse
 * - 管理 Curse
 *
 * 它只是 Curse 的来源信息。
 */
public interface CurseSource {

    /**
     * 获取来源类型。
     *
     * 例如：
     *
     * qisplan2:ghost_tombstone
     */
    ResourceLocation getType();

    /**
     * 保存来源自身的数据。
     *
     * 具体来源类型决定自己的 NBT 结构。
     */
    CompoundTag save();
}