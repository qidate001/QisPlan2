package com.qidate.qisplan2.event;

import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

public final class GhostLayerCombatHandler {

    private GhostLayerCombatHandler() {
    }

    @SubscribeEvent
    public static void onAttackEntity(
            AttackEntityEvent event
    ) {
        Entity attacker = event.getEntity();
        Entity target = event.getTarget();

        if (!GhostLayerHandler.canSee(
                attacker,
                target
        )) {
            event.setCanceled(true);
        }
    }
}