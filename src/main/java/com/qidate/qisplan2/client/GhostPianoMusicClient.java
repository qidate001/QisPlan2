package com.qidate.qisplan2.client;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class GhostPianoMusicClient {

    private static final Map<BlockPos, GhostPianoSoundInstance>
            PLAYING = new HashMap<>();

    private GhostPianoMusicClient() {
    }

    public static void start(
            BlockPos pos
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        if (PLAYING.containsKey(pos)) {
            return;
        }

        GhostPianoSoundInstance sound =
                new GhostPianoSoundInstance(pos);

        PLAYING.put(
                pos,
                sound
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 开始播放鬼音乐：{}",
                pos
        );

        minecraft.getSoundManager().play(sound);
    }

    public static void stop(
            BlockPos pos
    ) {
        GhostPianoSoundInstance sound =
                PLAYING.remove(pos);

        if (sound == null) {
            return;
        }

        sound.stopSound();

        Minecraft.getInstance()
                .getSoundManager()
                .stop(sound);

        QisPlan2.LOGGER.info(
                "[QisPlan2] 停止播放鬼音乐：{}",
                pos
        );
    }

    public static void stopAll() {
        for (GhostPianoSoundInstance sound :
                PLAYING.values()) {

            sound.stopSound();

            Minecraft.getInstance()
                    .getSoundManager()
                    .stop(sound);
        }

        PLAYING.clear();
    }
}