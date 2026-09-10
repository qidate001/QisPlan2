package com.qidate.qisplan2.ghost.domain;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class GhostDomainPlayerLogout {

    private GhostDomainPlayerLogout() {
    }

    @SubscribeEvent
    public static void onPlayerLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        GhostDomainManager
                .get(player.serverLevel())
                .removeBySource(player.getUUID());
    }
}