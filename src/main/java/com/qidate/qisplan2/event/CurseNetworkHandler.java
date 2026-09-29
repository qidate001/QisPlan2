package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.network.curse.CurseNetwork;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 诅咒系统网络同步事件。
 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class CurseNetworkHandler {

    private CurseNetworkHandler() {
    }

    /**
     * 玩家进入服务器时，
     * 向客户端同步当前诅咒状态。
     */
    @SubscribeEvent
    public static void onPlayerLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {

        if (!(event.getEntity()
                instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }

        CurseNetwork.sync(player);
    }
}