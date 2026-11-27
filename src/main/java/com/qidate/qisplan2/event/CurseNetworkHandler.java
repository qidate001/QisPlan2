package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.network.curse.CurseNetwork;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 诅咒系统网络同步事件。
 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class CurseNetworkHandler {

    /** 新玩家实体建立后，同步死亡策略执行后仍然保留的诅咒。 */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CurseNetwork.sync(player);
    }

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