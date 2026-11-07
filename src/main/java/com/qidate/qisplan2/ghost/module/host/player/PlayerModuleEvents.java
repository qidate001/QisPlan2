package com.qidate.qisplan2.ghost.module.host.player;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import com.qidate.qisplan2.ghost.possession.manager.GhostPossessionManager;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.ghost.possession.target.BlockGhostPossessionTarget;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** 玩家宿主的语义事件与离线资源清理。触碰采用进入判定，持续重叠不重复触发。 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class PlayerModuleEvents {
    private static final Map<ServerPlayer, Set<UUID>> TOUCHING = new WeakHashMap<>();
    public static void tickTouch(ServerPlayer player, PlayerGhostModuleHost host) {
        Set<UUID> old = TOUCHING.getOrDefault(player, Set.of());
        Set<UUID> current = new HashSet<>();
        for (var target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(0.05), e -> e != player && e.isAlive())) {
            current.add(target.getUUID());
            if (!old.contains(target.getUUID())) GhostModuleRuntime.dispatch(host, new TouchEvent(player, target));
            if (!player.isAlive()) break;
        }
        TOUCHING.put(player, current);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (var entry : new PlayerGhostModuleHost(player).getModules()) {
            var module = GhostModuleRegistry.get(entry.id());
            if (module != null) module.onPlayerSuspended(new PlayerGhostModuleContext(player, entry.id(),
                    PossessionHandler.getData(player, entry.id())));
        }
        TOUCHING.remove(player);
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        // 通常死亡破体后列表已空；若世界拒绝生成破体实体，保留未转移模块，避免丢失。
        if (event.getEntity() instanceof ServerPlayer player)
            PlayerGhostData.setModules(player, PlayerGhostData.getModules(event.getOriginal()));
    }
    @SubscribeEvent public static void blockInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().isEmpty()
                || !(event.getEntity() instanceof ServerPlayer player) || player.isShiftKeyDown()) return;
        var block = player.serverLevel().getBlockEntity(event.getPos());
        if (!(block instanceof GhostModuleDataModifier host)) return;
        var entry = host.getModules().stream().filter(e -> !GhostModuleCombatState.available(e)
                && !PossessionHandler.hasGhost(player, e.id())).findFirst().orElse(null);
        if (entry != null && GhostPossessionManager.start(player, new BlockGhostPossessionTarget(block, entry.id()))) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }
}
