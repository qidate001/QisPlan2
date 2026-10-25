package com.qidate.qisplan2.ghost.module.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * 灵异模块声明的声音能力定义。
 *
 * 声音类别用于服务端传播状态和灵异规则判定；
 * 声音事件 ID 用于客户端实际播放。
 *
 * 定义不绑定具体方块类型，也不保存播放状态。
 */
public record GhostSoundDefinition(
        ResourceLocation soundType,
        ResourceLocation soundEvent,
        double range,
        Predicate<BlockState> blocker
) {
    public GhostSoundDefinition {
        Objects.requireNonNull(soundType, "soundType");
        Objects.requireNonNull(soundEvent, "soundEvent");
        Objects.requireNonNull(blocker, "blocker");

        if (!Double.isFinite(range) || range < 0.0D) {
            throw new IllegalArgumentException(
                    "Sound range must be finite and non-negative"
            );
        }
    }
}