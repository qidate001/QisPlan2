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



    // 喊人鬼：普通玩家
    public static final DeferredHolder<SoundEvent, SoundEvent> CALLING_GHOST_PLAYER =
            SOUND_EVENTS.register(
                    "calling_ghost.player",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "calling_ghost.player"
                            )
                    )
            );

    // 喊人鬼：齐先生（QiNB_666）
    public static final DeferredHolder<SoundEvent, SoundEvent> CALLING_GHOST_SPECIAL_QI =
            SOUND_EVENTS.register(
                    "calling_ghost.special_qi",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "calling_ghost.special_qi"
                            )
                    )
            );

    // 喊人鬼：南镇（SouthTown_）
    public static final DeferredHolder<SoundEvent, SoundEvent> CALLING_GHOST_SPECIAL_SOUTH_TOWN =
            SOUND_EVENTS.register(
                    "calling_ghost.special_south_town",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "calling_ghost.south_town"
                            )
                    )
            );

    // 喊人鬼：佳楠（Jia_nan11）
    public static final DeferredHolder<SoundEvent, SoundEvent> CALLING_GHOST_SPECIAL_JIA_NAN =
            SOUND_EVENTS.register(
                    "calling_ghost.special_jia_nan",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "calling_ghost.special_jia_nan"
                            )
                    )
            );

    // 喊人鬼：军师（M_JS）
    public static final DeferredHolder<SoundEvent, SoundEvent> CALLING_GHOST_SPECIAL_M_JS =
            SOUND_EVENTS.register(
                    "calling_ghost.special_m_js",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "calling_ghost.special_m_js"
                            )
                    )
            );

    // 喊人鬼：特殊玩家 5
    public static final DeferredHolder<SoundEvent, SoundEvent> CALLING_GHOST_SPECIAL_UZJHF_836 =
            SOUND_EVENTS.register(
                    "calling_ghost.special_uzjhf_836",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    MODID,
                                    "calling_ghost.special_uzjhf_836"
                            )
                    )
            );
}
