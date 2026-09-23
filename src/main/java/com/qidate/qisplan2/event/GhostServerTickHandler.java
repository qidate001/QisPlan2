package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.GhostServerManager;
import com.qidate.qisplan2.ghost.possession.ability.knockingghost.KnockingGhostDoorSystem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = QisPlan2.MODID)
public final class GhostServerTickHandler {

    private GhostServerTickHandler() {}

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        // 鬼域 & 驭鬼者
        GhostServerManager.tick(
                event.getServer()
        );

        // 敲门鬼系统
        KnockingGhostDoorSystem.tick();
    }
}