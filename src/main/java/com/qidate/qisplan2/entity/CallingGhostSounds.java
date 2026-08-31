package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.core.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;

public final class CallingGhostSounds {

    private CallingGhostSounds() {
    }

    /**
     * 根据玩家名字选择喊人鬼音效。
     */
    public static SoundEvent getSound(
            ServerPlayer player
    ) {

        String playerName =
                player.getGameProfile().getName();

        return switch (playerName) {

            case "QiNB_666", "QiDate001", "Qi", "qi", "qinb" ->
                    ModSounds.CALLING_GHOST_SPECIAL_QI.get();

            case "SouthTown_" ->
                    ModSounds.CALLING_GHOST_SPECIAL_SOUTH_TOWN.get();

            case "Jia_nan11" ->
                    ModSounds.CALLING_GHOST_SPECIAL_JIA_NAN.get();

            case "M_JS" ->
                    ModSounds.CALLING_GHOST_SPECIAL_M_JS.get();

            case "uzjhf_836" ->
                    ModSounds.CALLING_GHOST_SPECIAL_UZJHF_836.get();
            /*
             * 默认音频。
             */
            default ->
                    ModSounds.CALLING_GHOST_PLAYER.get();
        };
    }
}