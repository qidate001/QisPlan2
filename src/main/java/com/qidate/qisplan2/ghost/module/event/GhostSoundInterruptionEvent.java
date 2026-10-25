package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * 玩家彻底失去某类灵异声音时产生的语义事件。
 *
 * 该事件必须由声音传播管理器在完成多声源聚合后产生。
 */
public record GhostSoundInterruptionEvent(
        ServerPlayer listener,
        ResourceLocation soundType
) implements GhostEvent {

    public GhostSoundInterruptionEvent {
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(soundType, "soundType");
    }
}