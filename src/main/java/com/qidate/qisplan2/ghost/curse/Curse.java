package com.qidate.qisplan2.ghost.curse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * 一个正在运行中的诅咒实例。
 *
 * Curse 表示的是一个“具体存在的诅咒”。
 *
 * 例如：
 *
 * 某个玩家
 * 被某一座鬼墓碑
 * 施加了一个鬼墓碑诅咒。
 *
 * 这就是一个 Curse 实例。
 *
 * Curse 本身只负责：
 *
 * 1. 保存自己的基本信息
 * 2. 执行每 Tick 的诅咒逻辑
 * 3. 判断自己是否仍然有效
 * 4. 保存自己的完整数据
 *
 * 创建和恢复由 CurseType 负责。
 */
public interface Curse {

    /**
     * 获取这个诅咒实例自己的唯一 ID。
     *
     * 每一个具体诅咒实例都必须拥有独立 UUID。
     */
    UUID getId();

    /**
     * 获取诅咒类型 ID。
     *
     * 例如：
     *
     * qisplan2:ghost_tombstone
     */
    ResourceLocation getType();

    /**
     * 获取被诅咒的目标玩家 UUID。
     */
    UUID getTarget();

    /**
     * 获取诅咒来源。
     *
     * 例如：
     *
     * 鬼墓碑
     * 鬼实体
     * 鬼物品
     * 鬼域
     */
    CurseSource getSource();

    /**
     * 每个服务器 Tick 执行一次。
     *
     * 具体诅咒效果由各自的 Curse 实现。
     */
    void tick(
            MinecraftServer server
    );

    /**
     * 判断当前诅咒是否仍然有效。
     *
     * 返回 false 后，
     * CurseManager 会自动移除这个诅咒。
     */
    boolean isValid(
            MinecraftServer server
    );

    /**
     * 保存这个诅咒实例的完整数据。
     *
     * 注意：
     *
     * 这里保存的是这个 Curse 自己的完整状态。
     *
     * CurseSavedData 不需要知道：
     *
     * - Id 怎么保存
     * - Target 怎么保存
     * - Source 怎么保存
     * - 诅咒内部还有什么额外数据
     *
     * 这些全部由具体 Curse 自己决定。
     *
     * CurseSavedData 只负责保存：
     *
     * Type + Data
     */
    CompoundTag save();
}