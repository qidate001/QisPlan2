package com.qidate.qisplan2.ghost.curse;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * 一个正在运行中的诅咒实例。
 *
 * Curse 表示的是一个具体存在的诅咒，
 * 而不是诅咒类型。
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
     * 获取被诅咒的目标。
     */
    UUID getTarget();

    /**
     * 获取诅咒来源。
     */
    CurseSource getSource();

    /**
     * 每个服务器 Tick 执行一次。
     */
    void tick(
            MinecraftServer server
    );

    /**
     * 判断诅咒是否仍然有效。
     *
     * 返回 false 后，
     * CurseManager 会自动注销。
     */
    boolean isValid(
            MinecraftServer server
    );
}