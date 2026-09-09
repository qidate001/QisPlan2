package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class GhostDomainPlayerSync {

    private GhostDomainPlayerSync() {
    }

    /**
     * 玩家加入服务器：
     * 将当前维度已经存在的所有鬼域补发给该玩家。
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
     * 玩家切换维度：
     *
     * 1. 删除玩家在旧维度留下的鬼域
     * 2. 向玩家同步新维度已经存在的鬼域
     *
     * 如果玩家还撑着鬼伞：
     * 下一次 ServerTick 会在新维度重新创建鬼域。
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        /*
         * 先找到旧维度。
         */
        ServerLevel oldLevel =
                player.server.getLevel(event.getFrom());

        /*
         * 删除玩家在旧维度留下的 GhostDomain。
         *
         * removeBySource() 内部会：
         *   - 从旧维度管理器删除
         *   - 广播 REMOVE 给旧维度所有玩家
         */
        if (oldLevel != null) {

            GhostDomainManager.get(oldLevel)
                    .removeBySource(player.getUUID());

            QisPlan2.LOGGER.info(
                    "[GhostDomain] 清理玩家旧维度鬼域: player={} dimension={}",
                    player.getGameProfile().getName(),
                    event.getFrom().location()
            );
        }

        /*
         * 再同步新维度已经存在的鬼域。
         */
        sync(player);

        QisPlan2.LOGGER.info(
                "[GhostDomain] 玩家切换维度 {} -> {}，补发鬼域: {}",
                event.getFrom().location(),
                event.getTo().location(),
                player.getGameProfile().getName()
        );
    }

    /**
     * 将玩家当前维度的所有 GhostDomain
     * 单独发送给该玩家。
     */
    private static void sync(ServerPlayer player) {

        GhostDomainManager manager =
                GhostDomainManager.get(player.serverLevel());

        manager.syncToPlayer(player);
    }
}