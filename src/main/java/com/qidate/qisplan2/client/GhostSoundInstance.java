package com.qidate.qisplan2.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class GhostSoundInstance extends AbstractTickableSoundInstance {

    public GhostSoundInstance(
            SoundEvent soundEvent,
            double x,
            double y,
            double z
    ) {
        super(soundEvent, SoundSource.BLOCKS, RandomSource.create());

        this.x = x;
        this.y = y;
        this.z = z;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.attenuation = Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        // 声音位置由服务端声源决定，无需在客户端移动。
    }
}