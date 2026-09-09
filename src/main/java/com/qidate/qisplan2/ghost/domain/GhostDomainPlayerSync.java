package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class GhostDomainPlayerSync {

    private GhostDomainPlayerSync() {
    }

    /**
     * 玩家刚加入服务器。
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(
            PlayerEvent.PlayerLoggedInEvent event
    ) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        sync(player);

        QisPlan2.LOGGER.info(
                "[GhostDomain] 玩家加入，补发鬼域: {}",
                player.getGameProfile().getName()
        );
    }

    /**
     * 玩家切换维度。
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        sync(player);

        QisPlan2.LOGGER.info(
                "[GhostDomain] 玩家切换维度 {} -> {}，补发鬼域: {}",
                event.getFrom().location(),
                event.getTo().location(),
                player.getGameProfile().getName()
        );
    }

    private static void sync(ServerPlayer player) {

        GhostDomainManager manager =
                GhostDomainManager.get(
                        player.serverLevel()
                );

        manager.syncToPlayer(player);
    }
}