
package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 不可触之鬼。
 *
 * 受到触碰事件时，以触碰来源作为灵异袭击来源，
 * 对触碰目标执行灵异袭击。
 */
public final class UntouchableGhostModule implements GhostModule {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "untouchable_ghost"
            );

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public boolean supports(GhostEvent event) {
        return event instanceof TouchEvent;
    }

    @Override
    public void onEvent(
            GhostModuleContext context,
            GhostEvent event
    ) {
        if (!(event instanceof TouchEvent touchEvent)) {
            return;
        }

        LivingEntity source = touchEvent.source();
        LivingEntity target = touchEvent.target();

        SupernaturalDeathHandler.tryKill(
                target,
                ModDamageTypes.untouchableGhost(source),
                context.intensity()
        );
    }
}