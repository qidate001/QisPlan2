package com.qidate.qisplan2.client.key;

import com.qidate.qisplan2.client.gui.PossessionScreen;
import com.qidate.qisplan2.network.QisNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import static com.qidate.qisplan2.client.key.ModKeyMappings.*;

public final class ClientKeyHandler {

    private ClientKeyHandler() {
    }

    public static void registerKeyMappings(
            RegisterKeyMappingsEvent event
    ) {

        event.register(OPEN_POSSESSION_SCREEN);

        event.register(GHOST_DIVINATION_LIFE);
        event.register(GHOST_DIVINATION_DEATH);
        event.register(GHOST_DIVINATION_GHOST);

        event.register(GHOST_EYE_LAYER_UP);
        event.register(GHOST_EYE_LAYER_DOWN);
        event.register(GHOST_EYE_SELF_LAYER_UP);
        event.register(GHOST_EYE_SELF_LAYER_DOWN);
        event.register(GHOST_EYE_TOGGLE);
        event.register(GHOST_EYE_REBOOT);

        event.register(GHOST_DOMAIN_TELEPORT);
        event.register(GHOST_DOMAIN_RAISE_LAYER);
        event.register(GHOST_DOMAIN_LOWER_LAYER);
    }

    public static void clientTick(
            ClientTickEvent.Post event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        while (OPEN_POSSESSION_SCREEN.consumeClick()) {

            if (minecraft.screen == null) {

                minecraft.setScreen(
                        new PossessionScreen()
                );
            }
        }


        /*
         * ========================================================
         * 鬼签
         * ========================================================
         */

        // 活签
        while (GHOST_DIVINATION_LIFE.consumeClick()) {
            QisNetwork.sendGhostDivinationUse(0);
        }

        // 死签
        while (GHOST_DIVINATION_DEATH.consumeClick()) {
            QisNetwork.sendGhostDivinationUse(1);
        }

        // 鬼签
        while (GHOST_DIVINATION_GHOST.consumeClick()) {
            QisNetwork.sendGhostDivinationUse(2);
        }

        /*
         * ========================================================
         * 鬼眼
         * ========================================================
         */

        // 提高鬼域总层数
        while (GHOST_EYE_LAYER_UP.consumeClick()) {
            QisNetwork.sendGhostEyeLayerChange(1);
        }

        // 降低鬼域总层数
        while (GHOST_EYE_LAYER_DOWN.consumeClick()) {
            QisNetwork.sendGhostEyeLayerChange(-1);
        }

        // 提高玩家自己所在鬼域层数
        while (GHOST_EYE_SELF_LAYER_UP.consumeClick()) {
            QisNetwork.sendGhostEyeSelfLayerChange(1);
        }

        // 降低玩家自己所在鬼域层数
        while (GHOST_EYE_SELF_LAYER_DOWN.consumeClick()) {
            QisNetwork.sendGhostEyeSelfLayerChange(-1);
        }

        // 开关鬼眼
        while (GHOST_EYE_TOGGLE.consumeClick()) {
            QisNetwork.sendGhostEyeToggle();
        }

        // 鬼眼重启
        if (GHOST_EYE_REBOOT.consumeClick()) {
            QisNetwork.sendGhostEyeReboot();
        }

        /*
         * ========================================================
         * 鬼域
         * ========================================================
         */

        // 化虹
        if (GHOST_DOMAIN_TELEPORT.consumeClick()) {
            QisNetwork.sendGhostDomainTeleport();
        }

        // 提升指向的生物所在层数
        if (GHOST_DOMAIN_RAISE_LAYER.consumeClick()) {
            QisNetwork.sendGhostDomainRaiseLayer();
        }

        // 降低指向的生物所在层数
        if (GHOST_DOMAIN_LOWER_LAYER.consumeClick()) {
            QisNetwork.sendGhostDomainLowerLayer();
        }
    }
}