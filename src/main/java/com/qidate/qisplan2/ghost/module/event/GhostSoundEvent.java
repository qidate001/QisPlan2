package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/**
 * 灵异声音语义事件。
 *
 * 用于描述灵异声音传播过程中发生的语义变化，
 * 可供鬼钢琴、八音盒及其他声音类灵异模块复用。
 *
 * 本事件不负责播放声音、检测传播范围或执行攻击。
 */
public record GhostSoundEvent(
        Type type,
        ResourceLocation soundType,
        UUID sourceId,
        UUID listenerId
) implements GhostEvent {

    public GhostSoundEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(soundType, "soundType");
    }

    /**
     * 声音传播过程中发生的语义变化。
     */
    public enum Type {

        /**
         * 声源开始产生声音。
         */
        STARTED,

        /**
         * 声源停止产生声音。
         */
        STOPPED,

        /**
         * 听众进入该声音的有效传播范围。
         */
        ENTERED,

        /**
         * 听众离开该声音的有效传播范围。
         */
        LEFT,

        /**
         * 声音传播被灵异规则中断。
         */
        INTERRUPTED
    }

    /**
     * 是否具有明确的声源。
     */
    public boolean hasSource() {
        return sourceId != null;
    }

    /**
     * 是否具有明确的听众。
     */
    public boolean hasListener() {
        return listenerId != null;
    }
}