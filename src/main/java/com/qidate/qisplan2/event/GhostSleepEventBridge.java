package com.qidate.qisplan2.event;

import com.qidate.qisplan2.ghost.module.host.GhostLivingHosts;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashSet;
import java.util.UUID;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.SleepTimeSkippedEvent;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;

/** 睡眠跳时只发布世界事实；附近所有生命载体均可接收，由模块决定是否响应。 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class GhostSleepEventBridge {
    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostSleepEventBridge() {}

    /**
     * 睡眠跳时完成后通知附近宿主，同一身体只通知一次。
     */
    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        // NeoForge 传入的是醒来后的绝对时间，减去当前时间才是实际跳过的时长。
        long skipped = Math.max(0, event.getNewTime() - level.getDayTime());
        if (skipped == 0) return;
        var notified = new HashSet<UUID>();
        for (var player : level.players()) {
            for (var body : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(128), LivingEntity::isAlive)) {
                if (notified.add(body.getUUID())) GhostModuleRuntime.dispatch(GhostLivingHosts.collect(body), new SleepTimeSkippedEvent(skipped));
            }
        }
    }
}
