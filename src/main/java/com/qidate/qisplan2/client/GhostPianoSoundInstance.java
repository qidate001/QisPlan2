package com.qidate.qisplan2.client;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

public class GhostPianoSoundInstance
        extends AbstractTickableSoundInstance {

    private final BlockPos pianoPos;

    private boolean stopped = false;

    public GhostPianoSoundInstance(
            BlockPos pianoPos
    ) {
        super(
                ModSounds.GHOST_PIANO_MUSIC.get(),
                SoundSource.BLOCKS,
                SoundInstance.createUnseededRandom()
        );

        this.pianoPos = pianoPos;

        this.x = pianoPos.getX() + 0.5D;
        this.y = pianoPos.getY() + 0.5D;
        this.z = pianoPos.getZ() + 0.5D;

        this.volume = 1.0F;
        this.pitch = 1.0F;

        this.looping = true;
        this.delay = 0;

        /*
         * 使用线性距离衰减。
         */
        this.attenuation =
                SoundInstance.Attenuation.LINEAR;

        QisPlan2.LOGGER.info(
                "[QisPlan2] 创建鬼音乐实例：{}",
                pianoPos
        );
    }

    public BlockPos getPianoPos() {
        return pianoPos;
    }

    public void stopSound() {
        stopped = true;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void tick() {

        if (stopped) {
            return;
        }

        /*
         * 这里暂时什么也不做。
         *
         * looping=true 会让声音持续循环。
         */
    }
}