
package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.event.BlockDestructionAttemptEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.resources.ResourceLocation;

import static com.qidate.qisplan2.QisPlan2.MODID;

public final class GhostStoneBricksModule implements GhostModule {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_stone_bricks"
            );

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public boolean supports(GhostEvent event) {
        return event instanceof BlockDestructionAttemptEvent;
    }

    @Override
    public void onEvent(
            GhostModuleContext context,
            GhostEvent event
    ) {
        if (!(event instanceof BlockDestructionAttemptEvent destructionEvent)) {
            return;
        }

        boolean killed = SupernaturalDeathHandler.tryKill(
                destructionEvent.getPlayer(),
                ModDamageTypes.ghostStoneBricks(
                        destructionEvent.getPlayer()
                ),
                context.intensity()
        );

        if (killed) {
            destructionEvent.cancel();
        }
    }
}
