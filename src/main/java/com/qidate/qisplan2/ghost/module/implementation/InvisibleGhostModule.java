package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.event.EntityPerceivedEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 不可视之鬼模块。
 *
 * 玩家看见携带该模块的鬼时，
 * 该鬼以模块强度发动灵异袭击。
 */
public final class InvisibleGhostModule implements GhostModule {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "invisible_ghost"
            );

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public boolean supports(GhostEvent event) {
        return event instanceof EntityPerceivedEvent;
    }

    @Override
    public void onEvent(
            GhostModuleContext context,
            GhostEvent event
    ) {
        if (!(event instanceof EntityPerceivedEvent perceivedEvent)) {
            return;
        }

        if (!(perceivedEvent.observer() instanceof Player player)) {
            return;
        }

        SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.invisibleGhost(
                        perceivedEvent.target()
                ),
                context.intensity()
        );
    }
}