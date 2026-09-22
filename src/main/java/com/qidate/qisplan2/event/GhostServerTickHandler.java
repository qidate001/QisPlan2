package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.GhostServerManager;
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
        GhostServerManager.tick(
                event.getServer()
        );
    }
}