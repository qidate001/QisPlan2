package com.qidate.qisplan2.client.key;

import com.qidate.qisplan2.client.gui.PossessionScreen;
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

        while (ModKeyMappings.GHOST_DIVINATION_LIFE.consumeClick()) {
        }

        while (ModKeyMappings.GHOST_DIVINATION_DEATH.consumeClick()) {
        }

        while (ModKeyMappings.GHOST_DIVINATION_GHOST.consumeClick()) {
        }
    }
}