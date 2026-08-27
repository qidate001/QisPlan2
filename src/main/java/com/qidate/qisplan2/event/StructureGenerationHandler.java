package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.structure.GhostLakeGenerationManager;
import com.qidate.qisplan2.structure.GhostManorGenerationManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class StructureGenerationHandler {

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {

        /*
         * 鬼庄园生成任务。
         */
        GhostManorGenerationManager.tick();

        /*
         * 鬼湖生成任务。
         */
        GhostLakeGenerationManager.tick();
    }
}