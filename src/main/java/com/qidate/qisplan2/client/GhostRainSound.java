package com.qidate.qisplan2.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class GhostRainSound
        extends AbstractTickableSoundInstance {

    public GhostRainSound() {
        super(
                SoundEvents.WEATHER_RAIN,
                SoundSource.WEATHER,
                RandomSource.create()
        );

        this.looping = true;

        this.volume = 0.75F;
        this.pitch = 1.0F;

        /*
         * 作为环境声播放。
         */
        this.relative = true;

        this.attenuation =
                SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {

        /*
         * 离开鬼雨领域后，
         * 由声音自己停止。
         */
        if (!GhostUmbrellaDomainClient.isInsideDomain()) {
            stopSound();
        }
    }

    /**
     * 公开的停止方法。
     */
    public void stopSound() {
        stop();
    }
}