package com.qidate.qisplan2.ghost.isolation.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * 客户端灵异隔绝生命周期事件。
 *
 * <p>
 * 负责清理客户端缓存的灵异隔绝区域，
 * 防止旧维度的数据残留到新维度。
 * </p>
 */
public final class ClientGhostIsolationEvents {

    private ClientGhostIsolationEvents() {
    }

    /**
     * 客户端退出服务器。
     *
     * <p>
     * 清除当前客户端缓存的所有灵异隔绝区域。
     * </p>
     *
     * @param event 客户端退出事件
     */
    @SubscribeEvent
    public static void onClientLogout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {

        ClientGhostIsolationManager.clear();
    }
}