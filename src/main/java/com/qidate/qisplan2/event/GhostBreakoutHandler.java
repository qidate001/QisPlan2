package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModEntities;
import com.qidate.qisplan2.death.QisDeathHandler;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class GhostBreakoutHandler {

    private GhostBreakoutHandler() {
    }

    /**
     * 复苏值与死机时间换算：
     *
     * 0%   -> 50 秒
     * 80%  -> 10 秒
     * 100% -> 0 秒
     */
    private static final long MAX_STUN_TICKS =
            1000L;


    @SubscribeEvent
    public static void onLivingDeath(
            LivingDeathEvent event
    ) {

        LivingEntity entity =
                event.getEntity();

        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        // 死亡相关的 QisPlan2 清理
        QisDeathHandler.cleanup(player);

        // 驭鬼者厉鬼破体
        breakout(player);
    }

    /**
     * 让玩家当前驾驭的所有厉鬼立即破体。
     *
     * 用于：
     *
     * 1. 正常死亡事件
     * 2. 厉鬼复苏达到 100% 的强制死亡
     */
    public static void breakout(ServerPlayer player) {
        var host = new PlayerGhostModuleHost(player);
        var entries = List.copyOf(host.getModules());
        for (var entry : entries) {
            var ghost = ModEntities.MODULAR_GHOST.get().create(player.serverLevel());
            if (ghost == null) continue;
            if (!ghost.importGhostModule(entry)) continue;
            var module = GhostModuleRegistry.get(entry.id());
            double max = module == null ? 100 : Math.max(1, module.getMaxRevival());
            int delay = (int)Math.max(0, Math.round((1 - entry.revival() / max) * MAX_STUN_TICKS));
            int previous = GhostModuleCombatState.ticks(entry);
            ghost.setSupernaturalStunTicks(Math.max(previous, delay));
            ghost.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            if (player.serverLevel().addFreshEntity(ghost)) {
                // 成功放入世界后才从玩家移除，避免创建失败时丢失模块。
                PossessionHandler.release(player, entry.id());
            }
        }
    }
}
