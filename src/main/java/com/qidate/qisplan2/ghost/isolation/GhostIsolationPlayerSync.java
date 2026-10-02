package com.qidate.qisplan2.ghost.isolation;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.network.ghostdomain.GhostDomainNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 灵异隔绝玩家同步。
 *
 * <p>
 * 负责处理玩家加入服务器以及切换维度时，
 * 将当前维度已经存在的灵异隔绝区域补发给玩家。
 * </p>
 *
 * <p>
 * 与 GhostIsolationSystem 的区别：
 * </p>
 *
 * <ul>
 *     <li>GhostIsolationSystem：负责服务端隔绝区域的检测与维护。</li>
 *     <li>GhostIsolationPlayerSync：负责将服务端已有数据同步给玩家客户端。</li>
 * </ul>
 */
public final class GhostIsolationPlayerSync {

    private GhostIsolationPlayerSync() {
    }

    /**
     * 玩家加入服务器。
     *
     * @param event 玩家加入事件
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
                "[灵异隔绝] 玩家加入，补发当前维度隔绝区域: {}",
                player.getGameProfile().getName()
        );
    }

    /**
     * 玩家切换维度。
     *
     * <p>
     * 先清除客户端旧维度的灵异隔绝区域，
     * 再补发新维度已经存在的隔绝区域。
     * </p>
     *
     * @param event 玩家切换维度事件
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        /*
         * 先清空客户端旧维度的隔绝区域缓存。
         */
        GhostDomainNetwork.sendIsolationClear(player);

        /*
         * 再同步新维度已经存在的隔绝区域。
         */
        sync(player);

        QisPlan2.LOGGER.info(
                "[灵异隔绝] 玩家切换维度 {} -> {}，清理旧缓存并补发新维度隔绝区域: {}",
                event.getFrom().location(),
                event.getTo().location(),
                player.getGameProfile().getName()
        );
    }

    /**
     * 将玩家当前维度已经确认隔绝的区域，
     * 全部单独发送给该玩家。
     *
     * @param player 目标玩家
     */
    private static void sync(ServerPlayer player) {
        GhostDomainNetwork.sendAllIsolationRegions(player);
    }
}