package com.qidate.qisplan2.entity.nightwanderer;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.SleepTimeSkippedEvent;
import com.qidate.qisplan2.ghost.module.implementation.nightwanderer.NightWandererModule;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;

/** 睡眠世界事件到模块语义事件的桥接，保留附近第一只可活动夜游鬼获得成长的范围。 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class NightWandererSleepHandler {
    private NightWandererSleepHandler() {}
    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        // NeoForge 传入的是醒来后的绝对时间，减去当前时间才是实际跳过的时长。
        long skipped = Math.max(0, event.getNewTime() - level.getDayTime());
        if (skipped == 0) return;
        for (var player : level.players()) {
            for (var host : level.getEntitiesOfClass(ModularGhostEntity.class,
                    player.getBoundingBox().inflate(128),
                    NightWandererModule::isActive)) {
                GhostModuleRuntime.dispatch(host, new SleepTimeSkippedEvent(skipped));
                return;
            }
        }
    }
}
