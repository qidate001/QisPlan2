package com.qidate.qisplan2.client.key;

import com.qidate.qisplan2.client.gui.PossessionScreen;
import com.qidate.qisplan2.network.QisNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class ClientKeyHandler {

    private ClientKeyHandler() {
    }

    public static void registerKeyMappings(
            RegisterKeyMappingsEvent event
    ) {

        event.register(ModKeyMappings.OPEN_POSSESSION_SCREEN);
        event.register(ModKeyMappings.GHOST_DIVINATION_LIFE);
        event.register(ModKeyMappings.GHOST_DIVINATION_DEATH);
        event.register(ModKeyMappings.GHOST_DIVINATION_GHOST);
    }

    public static void clientTick(
            ClientTickEvent.Post event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        while (ModKeyMappings.OPEN_POSSESSION_SCREEN.consumeClick()) {

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
        while (
                ModKeyMappings.GHOST_DIVINATION_LIFE
                        .consumeClick()
        ) {
            QisNetwork.sendGhostDivinationUse(0);
        }

        // 死签
        while (
                ModKeyMappings.GHOST_DIVINATION_DEATH
                        .consumeClick()
        ) {
            QisNetwork.sendGhostDivinationUse(1);
        }

        // 鬼签
        while (
                ModKeyMappings.GHOST_DIVINATION_GHOST
                        .consumeClick()
        ) {
            QisNetwork.sendGhostDivinationUse(2);
        }
    }
}