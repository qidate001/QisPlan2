package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** 宿主能力适配：模块不必为实体和玩家各维护一份相同的世界操作。 */
public final class GhostModuleHosts {
    private GhostModuleHosts() {}
    public static LivingEntity living(GhostModuleHost host) {
        if (host instanceof LivingEntity living) return living;
        return host instanceof PlayerGhostModuleHost player ? player.player() : null;
    }
    public static boolean available(GhostModuleHost host, ResourceLocation id) {
        var living = living(host);
        return living != null && living.isAlive() && !living.isRemoved()
                && (!(living instanceof ModularGhostEntity ghost) || !ghost.isCoffinNailed())
                && host.getModules().stream().anyMatch(e -> e.id().equals(id) && GhostModuleCombatState.available(e));
    }
    public static double strength(GhostModuleHost host, ResourceLocation id) {
        if (host instanceof PlayerGhostModuleHost player && player.player() instanceof ServerPlayer serverPlayer)
            return PossessionHandler.getEffectiveStrength(serverPlayer, id);
        return new GhostModuleContext(host, id).intensity();
    }
}
