package com.qidate.qisplan2.ghost.domain.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientGhostDomainEvents {

    private ClientGhostDomainEvents() {
    }

    @SubscribeEvent
    public static void onClientLogout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        ClientGhostDomainManager.clear();
    }
}