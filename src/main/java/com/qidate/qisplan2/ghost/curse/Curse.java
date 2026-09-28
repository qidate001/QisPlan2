package com.qidate.qisplan2.ghost.curse;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * 一个正在运行中的诅咒实例。
 *
 * Curse 表示的是“某一个具体的诅咒”，
 * 而不是诅咒类型本身。
 *
 * 例如：
 *
 * 某块鬼墓碑 → QiDate
 *
 * 就会产生一个具体的 Curse 实例。
 */
public interface Curse {

    /**
     * 获取这个诅咒实例自己的唯一 ID。
     */
    UUID getId();

    /**
     * 获取被诅咒的玩家 UUID。
     */
    UUID getTarget();

    /**
     * 每个服务器 Tick 执行一次。
     */
    void tick(MinecraftServer server);

    /**
     * 判断这个诅咒是否仍然有效。
     *
     * 返回 false 后，
     * CurseManager 会自动将它注销。
     */
    boolean isValid(MinecraftServer server);
}