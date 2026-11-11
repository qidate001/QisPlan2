package com.qidate.qisplan2.ghost.module.host.player;
import com.qidate.qisplan2.ghost.module.host.player.binding.GhostPlayerBindings;

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

/** 玩家宿主的语义事件与离线资源清理。持续接触报告时长，事件阶段区分进入与停留。 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class PlayerModuleEvents {
    private static final Map<ServerPlayer, Set<UUID>> TOUCHING = new WeakHashMap<>();

    /**
     * 检测玩家接触范围内的生命实体并发布一 tick 接触采样。
     * 进入与持续阶段分开标记，不可触规则只响应进入；玩家死亡后停止继续分发。
     */
    public static void tickTouch(ServerPlayer player, PlayerGhostModuleHost host) {
        Set<UUID> old = TOUCHING.getOrDefault(player, Set.of());
        Set<UUID> current = new HashSet<>();
        for (var target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(0.05), e -> e != player && e.isAlive())) {
            current.add(target.getUUID());
            GhostModuleRuntime.dispatch(host, TouchEvent.sample(player, target, !old.contains(target.getUUID())));
            if (!player.isAlive()) break;
        }
        TOUCHING.put(player, current);
    }

    /**
     * 玩家离线时通知各模块的适配器释放运行时资源，并清除接触记录。
     */
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (var entry : new PlayerGhostModuleHost(player).getModules()) {
            var module = GhostModuleRegistry.get(entry.id());
            if (module != null) GhostPlayerBindings.get(module).onPlayerSuspended(new PlayerGhostModuleContext(player, entry.id(),
                    PossessionHandler.getData(player, entry.id())));
        }
        TOUCHING.remove(player);
    }

    /**
     * 玩家重建时复制仍保留的模块数据，避免未能转移的模块丢失。
     */
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        // 通常死亡破体后列表已空；若世界拒绝生成破体实体，保留未转移模块，避免丢失。
        if (event.getEntity() instanceof ServerPlayer player)
            PlayerGhostData.setModules(player, PlayerGhostData.getModules(event.getOriginal()));
    }

    /**
     * 处理空主手与方块模块的驾驭交互。
     * 找到尚未驾驭的不可用模块后发起驾驭，成功受理时消费本次交互。
     */
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
