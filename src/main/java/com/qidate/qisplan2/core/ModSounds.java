package com.qidate.qisplan2.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.qidate.qisplan2.QisPlan2.MODID;
import static com.qidate.qisplan2.core.ModRegistries.SOUND_EVENTS;

public class ModSounds {

    private ModSounds() {}

    public static void init() {
        /*
         * 故意留空。
         *
         * 调用这个方法本身，就会强制 JVM
         * 在正确的时机完成 ModSounds 的静态初始化。
         */
    }





    // 鬼钢琴音乐
    public static final DeferredHolder<SoundEvent, SoundEvent> GHOST_PIANO_MUSIC =
            SOUND_EVENTS.register(
                    "ghost_piano_music",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "ghost_piano_music"
                            )
                    )
            );

    // 鬼黑雨
    public static final DeferredHolder<
            SoundEvent,
            SoundEvent
            > GHOST_KNOCK =
            SOUND_EVENTS.register(
                    "ghost_knock",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "ghost_knock"
                            )
                    )
            );
}
