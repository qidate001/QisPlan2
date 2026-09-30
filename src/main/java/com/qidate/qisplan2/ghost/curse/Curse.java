package com.qidate.qisplan2.ghost.curse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * 一个正在运行中的诅咒实例。
 *
 * <p>
 * Curse 表示的是一个具体存在的诅咒。
 *
 * <p>
 * 诅咒的通用属性：
 *
 * <ul>
 *     <li>唯一 ID</li>
 *     <li>诅咒类型</li>
 *     <li>诅咒目标</li>
 *     <li>诅咒来源</li>
 *     <li>诅咒强度</li>
 *     <li>剩余持续时间</li>
 * </ul>
 *
 * <p>
 * 具体诅咒的特殊状态和灵异逻辑，
 * 由具体 Curse 实现负责。
 */
public interface Curse {

    /**
     * 获取这个诅咒实例自己的唯一 ID。
     */
    UUID getId();

    /**
     * 获取诅咒类型 ID。
     */
    ResourceLocation getType();

    /**
     * 获取被诅咒的目标玩家 UUID。
     */
    UUID getTarget();

    /**
     * 获取诅咒来源。
     */
    CurseSource getSource();

    /**
     * 获取当前诅咒强度。
     */
    int getStrength();

    /**
     * 设置当前诅咒强度。
     */
    void setStrength(int strength);

    /**
     * 获取剩余持续时间。
     *
     * <p>
     * -1 表示无限持续。
     */
    int getRemainingTicks();

    /**
     * 设置剩余持续时间。
     *
     * <p>
     * -1 表示无限持续。
     */
    void setRemainingTicks(int ticks);

    /**
     * 每个服务器 Tick 执行一次。
     *
     * <p>
     * 具体诅咒效果由各自的 Curse 实现。
     */
    void tick(
            MinecraftServer server
    );

    /**
     * 判断当前诅咒是否仍然有效。
     *
     * <p>
     * 返回 false 后，
     * CurseManager 会自动移除这个诅咒。
     */
    boolean isValid(
            MinecraftServer server
    );

    /**
     * 保存这个诅咒实例的完整数据。
     */
    CompoundTag save();
}