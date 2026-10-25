package com.qidate.qisplan2.ghost.module.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * 通用鬼声音源的稳定身份。
 *
 * 同一维度、同一位置、同一声音类型对应同一个 ID。
 * 不同维度或不同声音类型不会发生身份冲突。
 */
public record GhostSoundSourceId(UUID value) {

    public GhostSoundSourceId {
        Objects.requireNonNull(value, "value");
    }

    public static GhostSoundSourceId forBlock(
            ResourceLocation dimension,
            BlockPos pos,
            ResourceLocation soundType
    ) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(soundType, "soundType");

        String identity = dimension + "|"
                + pos.getX() + ","
                + pos.getY() + ","
                + pos.getZ() + "|"
                + soundType;

        return new GhostSoundSourceId(
                UUID.nameUUIDFromBytes(
                        identity.getBytes(StandardCharsets.UTF_8)
                )
        );
    }
}