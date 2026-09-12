package com.qidate.qisplan2.ghost.domain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.*;

/**
 * 负责追踪实体与鬼域之间的关系。
 *
 * <p>服务器权威。</p>
 *
 * <p>该类负责处理：</p>
 * <ul>
 *     <li>实体进入鬼域</li>
 *     <li>实体离开鬼域</li>
 *     <li>实体从一个鬼域切换到另一个鬼域</li>
 *     <li>实体同时处于多个重叠鬼域</li>
 *     <li>根据鬼域优先级确定最终生效鬼域</li>
 * </ul>
 */
public final class GhostDomainEntityTracker {

    private final ServerLevel level;

    /**
     * Entity UUID → 当前最终生效的鬼域
     */
    private final Map<UUID, UUID> effectiveDomains =
            new HashMap<>();

    public GhostDomainEntityTracker(
            ServerLevel level
    ) {
        this.level = level;
    }

    /**
     * 更新一个实体当前所处的鬼域状态。
     */
    public void update(
            Entity entity
    ) {

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        GhostDomain previousDomain =
                getEffectiveDomain(entity);

        GhostDomain currentDomain =
                manager.getEffectiveDomain(entity);

        UUID entityUUID =
                entity.getUUID();

        /*
         * 没有发生变化。
         */
        if (sameDomain(
                previousDomain,
                currentDomain
        )) {
            return;
        }

        /*
         * 更新记录。
         */
        if (currentDomain == null) {

            effectiveDomains.remove(
                    entityUUID
            );

        } else {

            effectiveDomains.put(
                    entityUUID,
                    currentDomain.getId()
            );
        }

        /*
         * 根据状态变化触发事件。
         */
        if (previousDomain == null
                && currentDomain != null) {

            onEnter(
                    entity,
                    currentDomain
            );

            return;
        }

        if (previousDomain != null
                && currentDomain == null) {

            onLeave(
                    entity,
                    previousDomain
            );

            return;
        }

        /*
         * 两边都有：
         *
         * A → B
         *
         * 说明最终生效鬼域发生了切换。
         */
        if (previousDomain != null
                && currentDomain != null) {

            onSwitch(
                    entity,
                    previousDomain,
                    currentDomain
            );
        }
    }

    /**
     * 获取实体当前最终生效的鬼域。
     */
    public GhostDomain getEffectiveDomain(
            Entity entity
    ) {

        UUID domainId =
                effectiveDomains.get(
                        entity.getUUID()
                );

        if (domainId == null) {
            return null;
        }

        return GhostDomainManager
                .get(level)
                .get(domainId);
    }

    private boolean sameDomain(
            GhostDomain a,
            GhostDomain b
    ) {

        if (a == null && b == null) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        return a.getId().equals(
                b.getId()
        );
    }

    private void onEnter(
            Entity entity,
            GhostDomain domain
    ) {
        // 下一步实现
    }

    private void onLeave(
            Entity entity,
            GhostDomain domain
    ) {
        // 下一步实现
    }

    private void onSwitch(
            Entity entity,
            GhostDomain oldDomain,
            GhostDomain newDomain
    ) {
        // 下一步实现
    }
}