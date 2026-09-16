package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    /**
     * Entity UUID → 当前覆盖该实体的所有鬼域
     */
    private final Map<UUID, Set<UUID>> overlappingDomains =
            new HashMap<>();

    /**
     * GhostDomain UUID → 当前处于该鬼域中的实体 UUID
     */
    private final Map<UUID, Set<UUID>> domainEntities =
            new HashMap<>();

    private static final Map<
            ServerLevel,
            GhostDomainEntityTracker
            > TRACKERS =
            new WeakHashMap<>();

    public static GhostDomainEntityTracker get(
            ServerLevel level
    ) {

        return TRACKERS.computeIfAbsent(
                level,
                GhostDomainEntityTracker::new
        );
    }

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

        UUID entityUUID =
                entity.getUUID();

        /*
         * 找出实体当前覆盖的所有鬼域。
         */
        Set<UUID> currentDomains =
                new LinkedHashSet<>();

        for (GhostDomain domain :
                manager.getDomains()) {

            if (domain.contains(
                    entity.getX(),
                    entity.getY(),
                    entity.getZ()
            )) {

                currentDomains.add(
                        domain.getId()
                );
            }
        }

        /*
         * 获取实体之前覆盖的所有鬼域。
         */
        Set<UUID> previousDomains =
                overlappingDomains.get(
                        entityUUID
                );

        if (previousDomains == null) {
            previousDomains =
                    Collections.emptySet();
        }

        /*
         * 更新反向索引。
         *
         * 新增：
         *   实体进入了某个鬼域
         *
         * 删除：
         *   实体离开了某个鬼域
         */
        for (UUID domainId : currentDomains) {

            if (!previousDomains.contains(domainId)) {

                domainEntities
                        .computeIfAbsent(
                                domainId,
                                ignored -> new LinkedHashSet<>()
                        )
                        .add(entityUUID);

                if (entity instanceof ServerPlayer) {
                    QisPlan2.LOGGER.info(
                            "[GhostDomainTracker] 实体进入鬼域: entity={}, domain={}",
                            entity.getName().getString(),
                            domainId
                    );
                }
            }
        }

        for (UUID domainId : previousDomains) {

            if (!currentDomains.contains(domainId)) {

                Set<UUID> entities =
                        domainEntities.get(domainId);

                if (entities == null) {
                    continue;
                }

                entities.remove(entityUUID);

                if (entities.isEmpty()) {
                    domainEntities.remove(domainId);
                }
            }
        }

        /*
         * 保存当前覆盖关系。
         */
        if (currentDomains.isEmpty()) {

            overlappingDomains.remove(
                    entityUUID
            );

        } else {

            overlappingDomains.put(
                    entityUUID,
                    currentDomains
            );
        }

        /*
         * 根据当前所有鬼域重新计算最终鬼域。
         */
        GhostDomain previousEffective =
                getEffectiveDomain(
                        previousDomains
                );

        GhostDomain currentEffective =
                getEffectiveDomain(
                        currentDomains
                );

        /*
         * 最终鬼域没有发生变化。
         */
        if (sameDomain(
                previousEffective,
                currentEffective
        )) {
            return;
        }

        /*
         * 更新最终鬼域。
         */
        if (currentEffective == null) {

            effectiveDomains.remove(
                    entityUUID
            );

        } else {

            effectiveDomains.put(
                    entityUUID,
                    currentEffective.getId()
            );
        }

        /*
         * 进入鬼域。
         */
        if (previousEffective == null
                && currentEffective != null) {

            onEnter(
                    entity,
                    currentEffective
            );

            return;
        }

        /*
         * 离开所有鬼域。
         */
        if (previousEffective != null
                && currentEffective == null) {

            onLeave(
                    entity,
                    previousEffective
            );

            return;
        }

        /*
         * 最终生效鬼域发生切换。
         */
        if (previousEffective != null
                && currentEffective != null) {

            onSwitch(
                    entity,
                    previousEffective,
                    currentEffective
            );
        }
    }

    /**
     * 当鬼域被删除时，清理该鬼域对应的实体追踪数据。
     *
     * <p>注意：此时该鬼域已经从
     * {@link GhostDomainManager} 中删除，
     * 因此这里必须直接接收被删除的鬼域对象，
     * 以便正确触发生命周期回调。</p>
     *
     * @param removedDomain 被删除的鬼域
     */
    public void removeDomain(
            GhostDomain removedDomain
    ) {

        UUID domainId =
                removedDomain.getId();

        Set<UUID> entityUUIDs =
                domainEntities.remove(domainId);

        if (entityUUIDs == null
                || entityUUIDs.isEmpty()) {
            return;
        }

        /*
         * 复制一份，避免后续修改内部集合时
         * 产生 ConcurrentModificationException。
         */
        Set<UUID> affectedEntities =
                new LinkedHashSet<>(entityUUIDs);

        for (UUID entityUUID : affectedEntities) {

            Entity entity =
                    level.getEntity(entityUUID);

            /*
             * 获取实体之前的最终生效鬼域。
             */
            UUID previousEffectiveId =
                    effectiveDomains.get(entityUUID);

            boolean wasEffective =
                    domainId.equals(
                            previousEffectiveId
                    );

            /*
             * 获取实体当前覆盖的所有鬼域。
             */
            Set<UUID> domains =
                    overlappingDomains.get(
                            entityUUID
                    );

            /*
             * 理论上一定存在，但为了防止追踪数据异常，
             * 这里安全处理。
             */
            if (domains == null) {

                if (wasEffective) {

                    effectiveDomains.remove(
                            entityUUID
                    );

                    if (entity != null) {

                        removedDomain
                                .getBehavior()
                                .onEntityLeave(
                                        level,
                                        removedDomain,
                                        entity
                                );
                    }
                }

                continue;
            }

            /*
             * 移除已经不存在的鬼域。
             */
            domains.remove(domainId);

            /*
             * ========================================================
             * 情况一：
             * 实体已经不处于任何鬼域
             * ========================================================
             */
            if (domains.isEmpty()) {

                overlappingDomains.remove(
                        entityUUID
                );

                effectiveDomains.remove(
                        entityUUID
                );

                /*
                 * 只有被删除的鬼域原本是最终生效鬼域，
                 * 才需要触发离开事件。
                 */
                if (wasEffective
                        && entity != null) {

                    removedDomain
                            .getBehavior()
                            .onEntityLeave(
                                    level,
                                    removedDomain,
                                    entity
                            );
                }

                continue;
            }

            /*
             * ========================================================
             * 情况二：
             * 仍然处于其它鬼域
             * ========================================================
             */

            GhostDomain newEffective =
                    getEffectiveDomain(domains);

            /*
             * 被删除的鬼域不是最终生效鬼域。
             *
             * 那么实体的最终鬼域没有变化，
             * 不需要触发生命周期事件。
             */
            if (!wasEffective) {
                continue;
            }

            /*
             * 被删除的鬼域原本是最终生效鬼域，
             * 因此现在必须切换到新的最终鬼域。
             */
            if (newEffective == null) {

                effectiveDomains.remove(
                        entityUUID
                );

                if (entity != null) {

                    removedDomain
                            .getBehavior()
                            .onEntityLeave(
                                    level,
                                    removedDomain,
                                    entity
                            );
                }

                continue;
            }

            /*
             * 更新最终生效鬼域。
             */
            effectiveDomains.put(
                    entityUUID,
                    newEffective.getId()
            );

            /*
             * 通知新鬼域：
             *
             * 旧鬼域 → 新鬼域
             */
            if (entity != null) {

                newEffective
                        .getBehavior()
                        .onEntitySwitch(
                                level,
                                removedDomain,
                                newEffective,
                                entity
                        );
            }
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

    private GhostDomain getEffectiveDomain(
            Set<UUID> domainIds
    ) {

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        GhostDomain effectiveDomain = null;

        for (UUID domainId : domainIds) {

            GhostDomain domain =
                    manager.get(domainId);

            if (domain == null) {
                continue;
            }

            if (effectiveDomain == null) {

                effectiveDomain = domain;

                continue;
            }

            if (GhostDomainPriority.canOverride(
                    domain,
                    effectiveDomain
            )) {

                effectiveDomain = domain;
            }
        }

        return effectiveDomain;
    }

    public Set<GhostDomain> getDomains(
            Entity entity
    ) {

        Set<UUID> domainIds =
                overlappingDomains.get(
                        entity.getUUID()
                );

        if (domainIds == null
                || domainIds.isEmpty()) {

            return Collections.emptySet();
        }

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        Set<GhostDomain> result =
                new LinkedHashSet<>();

        for (UUID domainId : domainIds) {

            GhostDomain domain =
                    manager.get(domainId);

            if (domain != null) {
                result.add(domain);
            }
        }

        return Collections.unmodifiableSet(
                result
        );
    }

    /**
     * 获取当前处于指定鬼域中的所有实体。
     *
     * @param domain 鬼域
     * @return 当前处于该鬼域中的实体
     */
    public Set<Entity> getEntities(
            GhostDomain domain
    ) {

        Set<UUID> entityUUIDs =
                domainEntities.get(
                        domain.getId()
                );

        if (entityUUIDs == null
                || entityUUIDs.isEmpty()) {

            return Collections.emptySet();
        }

        Set<Entity> result =
                new LinkedHashSet<>();

        for (UUID entityUUID : entityUUIDs) {

            Entity entity =
                    level.getEntity(
                            entityUUID
                    );

            if (entity != null) {
                result.add(entity);
            }
        }

        return Collections.unmodifiableSet(
                result
        );
    }

    public boolean isInside(
            Entity entity,
            ResourceLocation type
    ) {

        for (GhostDomain domain :
                getDomains(entity)) {

            if (type.equals(
                    domain.getType()
            )) {
                return true;
            }
        }

        return false;
    }

    /**
     * 当鬼域层数发生变化时，
     * 刷新当前处于该鬼域中的实体层数。
     *
     * <p>只有当前最终生效鬼域就是该鬼域的实体，
     * 才会受到影响。</p>
     *
     * @param domain 层数发生变化的鬼域
     */
    public void refreshDomainLayer(
            GhostDomain domain,
            int oldLayer
    ) {

        Set<UUID> entityUUIDs =
                domainEntities.get(
                        domain.getId()
                );

        if (entityUUIDs == null
                || entityUUIDs.isEmpty()) {
            return;
        }

        Set<UUID> affectedEntities =
                new LinkedHashSet<>(entityUUIDs);

        for (UUID entityUUID : affectedEntities) {

            UUID effectiveDomainId =
                    effectiveDomains.get(
                            entityUUID
                    );

            /*
             * 只有这个鬼域当前真正生效，
             * 才处理实体。
             */
            if (!domain.getId().equals(
                    effectiveDomainId
            )) {
                continue;
            }

            Entity entity =
                    level.getEntity(entityUUID);

            if (entity == null) {
                continue;
            }

            /*
             * ========================================================
             * 总层数增加
             * ========================================================
             *
             * 默认不提升实体所在层数。
             */
            if (domain.getLayer() > oldLayer) {

                if (domain.getBehavior()
                        .shouldRaiseEntityLayer(
                                level,
                                domain,
                                entity
                        )) {

                    domain.getBehavior()
                            .onEntityLayerChange(
                                    level,
                                    domain,
                                    entity
                            );
                }

                continue;
            }

            /*
             * ========================================================
             * 总层数降低
             * ========================================================
             *
             * 只有实体当前层数超过新的总层数，
             * 才需要进行修正。
             */
            if (domain.getLayer() < oldLayer) {

                int entityLayer =
                        GhostLayerHandler.getLayer(
                                entity
                        );

                if (entityLayer <= domain.getLayer()) {
                    continue;
                }

                domain.getBehavior()
                        .onEntityLayerChange(
                                level,
                                domain,
                                entity
                        );
            }
        }
    }

    /**
     * 判断实体当前是否处于指定鬼域中。
     */
    public boolean isInside(
            Entity entity,
            GhostDomain domain
    ) {

        Set<UUID> domains =
                overlappingDomains.get(
                        entity.getUUID()
                );

        if (domains == null) {
            return false;
        }

        return domains.contains(
                domain.getId()
        );
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
        domain.getBehavior().onEntityEnter(
                level,
                domain,
                entity
        );
    }

    private void onLeave(
            Entity entity,
            GhostDomain domain
    ) {
        domain.getBehavior().onEntityLeave(
                level,
                domain,
                entity
        );
    }

    private void onSwitch(
            Entity entity,
            GhostDomain oldDomain,
            GhostDomain newDomain
    ) {
        newDomain.getBehavior().onEntitySwitch(
                level,
                oldDomain,
                newDomain,
                entity
        );
    }
}