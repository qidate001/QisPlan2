package com.qidate.qisplan2.ghost.layer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class GhostLayerInteractionHandler {

    private GhostLayerInteractionHandler() {
    }

    @SubscribeEvent
    public static void onEntityInteract(
            PlayerInteractEvent.EntityInteract event
    ) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

        if (!GhostLayerHandler.canSee(player, target)) {
            event.setCanceled(true);
        }
    }
}