package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * 灵异声音事件。
 *
 * 声音由模块宿主发出，事件通过 GhostModuleRuntime
 * 派发给发声宿主，因此不需要在事件中重复记录声源 UUID。
 *
 * 该事件描述声音与听众之间的传播状态变化。
 * 它本身不负责检测距离、遮挡或声音传播范围。
 */
public record GhostSoundEvent(
        Type type,
        ResourceLocation soundType,
        ServerPlayer listener
) implements GhostEvent {

    public GhostSoundEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(soundType, "soundType");
        Objects.requireNonNull(listener, "listener");
    }

    /**
     * 声音传播过程中的语义状态。
     */
    public enum Type {

        /** 声音开始播放。 */
        STARTED,

        /** 听众进入声音的有效传播范围。 */
        ENTERED,

        /** 听众离开声音的有效传播范围。 */
        LEFT,

        /** 声音传播被外部条件打断。 */
        INTERRUPTED,

        /** 发声宿主停止播放声音。 */
        STOPPED
    }
}