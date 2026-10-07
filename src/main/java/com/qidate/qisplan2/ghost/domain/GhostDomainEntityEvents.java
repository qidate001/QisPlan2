package com.qidate.qisplan2.ghost.domain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;

public final class GhostDomainEntityEvents {

    private GhostDomainEntityEvents() {
    }

    /**
     * 实体进入新的 Section 时，
     * 重新计算该实体与鬼域之间的关系。
     *
     * <p>
     * 这是鬼域实体追踪的事件驱动入口之一。
     * </p>
     */
    @SubscribeEvent
    public static void onEntityEnteringSection(
            EntityEvent.EnteringSection event
    ) {

        Entity entity = event.getEntity();

        /*
         * 只处理服务器实体。
         */
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        /*
         * 不参与鬼域追踪的实体直接忽略。
         */
        if (!GhostDomainEntityPolicy.shouldTrack(entity)) {
            return;
        }

        /*
         * 只有真正跨 Chunk 时才处理。
         *
         * 同一个 Chunk 内跨 Section，
         * 暂时不触发鬼域关系重新计算。
         */
        if (!event.didChunkChange()) {
            return;
        }

        GhostDomainEntityTracker
                .get(level)
                .update(entity);
    }
}