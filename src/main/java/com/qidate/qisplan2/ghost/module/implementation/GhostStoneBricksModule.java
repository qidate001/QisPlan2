package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.event.BlockDestructionAttemptEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.ItemDestructionAttemptEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

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
        return event instanceof BlockDestructionAttemptEvent
                || event instanceof ItemDestructionAttemptEvent;
    }

    @Override
    public void onEvent(
            GhostModuleContext context,
            GhostEvent event
    ) {
        if (event instanceof BlockDestructionAttemptEvent destructionEvent) {
            handleBlockDestruction(context, destructionEvent);
            return;
        }

        if (event instanceof ItemDestructionAttemptEvent destructionEvent) {
            handleItemDestruction(context, destructionEvent);
        }
    }

    /**
     * 鬼石砖方块：沿用原有规则。
     */
    private void handleBlockDestruction(
            GhostModuleContext context,
            BlockDestructionAttemptEvent event
    ) {
        Player player = event.getPlayer();

        boolean killed = SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostStoneBricks(player),
                context.intensity()
        );

        if (killed) {
            event.cancel();
        }
    }

    /**
     * 携带鬼石砖模块的物品：
     * 只有袭击失败，物品才允许因耐久耗尽而损坏。
     */
    private void handleItemDestruction(
            GhostModuleContext context,
            ItemDestructionAttemptEvent event
    ) {
        if (!(event.getHolder() instanceof Player player)) {
            return;
        }

        boolean killed = SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostStoneBricks(player),
                context.intensity()
        );

        if (killed) {
            event.cancel();
        }
    }
}