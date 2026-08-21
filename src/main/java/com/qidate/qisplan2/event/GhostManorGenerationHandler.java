package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.structure.GhostManorGenerationManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class GhostManorGenerationHandler {

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        GhostManorGenerationManager.tick();
    }
}