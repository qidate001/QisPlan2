package com.qidate.qisplan2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class GhostSoundClient {

    private static final Map<SoundKey, GhostSoundInstance> PLAYING =
            new HashMap<>();

    private GhostSoundClient() {
    }

    public static void start(
            UUID sourceId,
            ResourceLocation dimension,
            ResourceLocation soundEventId,
            double x,
            double y,
            double z
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null
                || !minecraft.level.dimension().location().equals(dimension)) {
            return;
        }

        SoundKey key = new SoundKey(dimension, sourceId);

        // 同一声源的重复播放请求不能创建重复实例。
        if (PLAYING.containsKey(key)) {
            return;
        }

        SoundEvent soundEvent =
                BuiltInRegistries.SOUND_EVENT.get(soundEventId);

        if (soundEvent == null
                || BuiltInRegistries.SOUND_EVENT.getKey(soundEvent) == null
                || !soundEventId.equals(
                BuiltInRegistries.SOUND_EVENT.getKey(soundEvent))) {
            return;
        }

        GhostSoundInstance instance =
                new GhostSoundInstance(soundEvent, x, y, z);

        PLAYING.put(key, instance);
        minecraft.getSoundManager().play(instance);
    }

    public static void stop(
            UUID sourceId,
            ResourceLocation dimension
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        SoundKey key = new SoundKey(dimension, sourceId);

        GhostSoundInstance instance = PLAYING.remove(key);

        if (instance != null) {
            minecraft.getSoundManager().stop(instance);
        }
    }

    /**
     * 停止所有声音。
     * 用于客户端切换世界时清理残留实例。
     */
    public static void stopAll() {
        Minecraft minecraft = Minecraft.getInstance();

        for (GhostSoundInstance instance : PLAYING.values()) {
            minecraft.getSoundManager().stop(instance);
        }

        PLAYING.clear();
    }

    private record SoundKey(
            ResourceLocation dimension,
            UUID sourceId
    ) {
        private SoundKey {
            Objects.requireNonNull(dimension);
            Objects.requireNonNull(sourceId);
        }
    }
}