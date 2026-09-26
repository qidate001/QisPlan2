package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.network.QisNetwork;
import com.qidate.qisplan2.network.ghostdomain.GhostDomainNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;

public final class GhostDomainManager {

    private static final Map<ServerLevel, GhostDomainManager>
            MANAGERS = new WeakHashMap<>();

    private final ServerLevel level;

    private final Map<UUID, GhostDomain> domains =
            new LinkedHashMap<>();

    /**
     * 当前由鬼域系统授予飞行权限的玩家。
     *
     * <p>
     * 用于区分：
     *
     * <ul>
     *     <li>玩家本身就拥有的 Minecraft 飞行权限</li>
     *     <li>鬼域系统额外授予的飞行权限</li>
     * </ul>
     *
     * <p>
     * 只有由鬼域系统授予的权限，
     * 才允许由鬼域系统回收。
     */
    private final Set<UUID> flightOwners =
            new HashSet<>();

    private GhostDomainManager(
            ServerLevel level
    ) {
        this.level = level;
    }

    public static GhostDomainManager get(
            ServerLevel level
    ) {
        return MANAGERS.computeIfAbsent(
                level,
                GhostDomainManager::new
        );
    }

    public void add(GhostDomain domain) {

        domains.put(domain.getId(), domain);

        domain.getBehavior().onCreate(
                level,
                domain
        );

        GhostDomainNetwork.sendAdd(
                level,
                domain
        );
    }

    public void syncToPlayer(ServerPlayer player) {

        for (GhostDomain domain : domains.values()) {

            GhostDomainNetwork.sendAdd(
                    player,
                    domain
            );
        }

        QisPlan2.LOGGER.info(
                "[GhostDomain] SYNC {} domains to player {}",
                domains.size(),
                player.getGameProfile().getName()
        );
    }

    public void remove(UUID id) {

        GhostDomain domain = domains.remove(id);

        if (domain == null) {
            return;
        }

        GhostDomainEntityTracker
                .get(level)
                .removeDomain(domain);

        domain.getBehavior().onRemove(
                level,
                domain
        );

        Entity source =
                level.getEntity(domain.getSourceUUID());

        if (source instanceof ServerPlayer player) {
            GhostDomainNetwork.sendVision(
                    player,
                    domain,
                    Map.of()
            );
        }

        GhostDomainNetwork.sendRemove(
                level,
                domain.getId()
        );
    }

    public void removeBySource(UUID sourceUUID) {

        List<UUID> remove = new ArrayList<>();

        for (GhostDomain domain : domains.values()) {
            if (sourceUUID.equals(domain.getSourceUUID())) {
                remove.add(domain.getId());
            }
        }

        for (UUID id : remove) {
            remove(id);
        }
    }

    public void removeBySourceAndType(
            UUID sourceUUID,
            ResourceLocation type
    ) {

        List<UUID> remove = new ArrayList<>();

        for (GhostDomain domain : domains.values()) {

            if (sourceUUID.equals(domain.getSourceUUID())
                    && type.equals(domain.getType())) {

                remove.add(domain.getId());
            }
        }

        for (UUID id : remove) {
            remove(id);
        }
    }

    public void updatePosition(
            UUID domainId,
            double x,
            double y,
            double z
    ) {
        GhostDomain domain = domains.get(domainId);

        if (domain == null) {
            return;
        }

        if (domain.getUpdateMode() == GhostDomainUpdateMode.MANUAL) {

            domain.setPosition(x, y, z);

            GhostDomainNetwork.sendUpdate(
                    level,
                    domain,
                    false
            );

            return;
        }

        double dx = x - domain.getX();
        double dy = y - domain.getY();
        double dz = z - domain.getZ();

        double distanceSquared =
                dx * dx + dy * dy + dz * dz;

        double threshold =
                domain.getUpdateDistance();

        if (distanceSquared < threshold * threshold) {
            return;
        }

        domain.setPosition(x, y, z);

        GhostDomainNetwork.sendUpdate(
                level,
                domain,
                false
        );
    }

    /**
     * 立即更新鬼域位置。
     *
     * <p>用于玩家传送等需要瞬间同步的情况。
     * 客户端不会经过平滑移动，直接跳转到新位置。</p>
     */
    public void updatePositionImmediate(
            UUID domainId,
            double x,
            double y,
            double z
    ) {
        GhostDomain domain = domains.get(domainId);

        if (domain == null) {
            return;
        }

        domain.setPosition(
                x,
                y,
                z
        );

        GhostDomainNetwork.sendUpdate(
                level,
                domain,
                true
        );
    }

    public void updateLayer(
            UUID domainId,
            int layer
    ) {

        GhostDomain domain =
                domains.get(domainId);

        if (domain == null) {
            return;
        }

        int oldLayer =
                domain.getLayer();

        if (oldLayer == layer) {
            return;
        }

        domain.setLayer(layer);

        GhostDomainEntityTracker
                .get(level)
                .refreshDomainLayer(
                        domain,
                        oldLayer
                );

        GhostDomainNetwork.sendUpdate(
                level,
                domain,
                false
        );
    }

    public GhostDomain get(
            UUID id
    ) {
        return domains.get(id);
    }

    public GhostDomain getBySource(
            UUID sourceUUID
    ) {

        for (GhostDomain domain : domains.values()) {

            if (sourceUUID.equals(
                    domain.getSourceUUID()
            )) {
                return domain;
            }
        }

        return null;
    }

    public GhostDomain getBySourceAndType(
            UUID sourceUUID,
            ResourceLocation type
    ) {

        for (GhostDomain domain : domains.values()) {

            if (sourceUUID.equals(domain.getSourceUUID())
                    && type.equals(domain.getType())) {

                return domain;
            }
        }

        return null;
    }

    public GhostDomain getEffectiveDomain(
            double x,
            double y,
            double z
    ) {

        GhostDomain effectiveDomain = null;

        for (GhostDomain domain : domains.values()) {

            if (!domain.contains(x, y, z)) {
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

    /**
     * 获取实体当前所处的最终鬼域。
     *
     * <p>实体如果同时处于多个鬼域中，
     * 会按照 {@link GhostDomainPriority} 的规则
     * 决定最终生效的鬼域。</p>
     *
     * @param entity 要判断的实体
     * @return 实体当前所处的最终鬼域；如果不在任何鬼域中则返回 null
     */
    public GhostDomain getEffectiveDomain(
            Entity entity
    ) {
        return getEffectiveDomain(
                entity.getX(),
                entity.getY(),
                entity.getZ()
        );
    }

    public Collection<GhostDomain> getDomains() {
        return Collections.unmodifiableCollection(
                domains.values()
        );
    }

    public void tick() {

        for (GhostDomain domain : domains.values()) {

            /*
             * ========================================================
             * 自动更新鬼域位置
             * ========================================================
             *
             * DISTANCE：
             *   鬼域自动跟随自己的来源实体。
             *
             * MANUAL：
             *   不自动移动，由外部主动调用 updatePosition()。
             */
            if (domain.getUpdateMode()
                    == GhostDomainUpdateMode.DISTANCE) {

                Entity source =
                        level.getEntity(
                                domain.getSourceUUID()
                        );

                if (source != null) {

                    updatePosition(
                            domain.getId(),
                            source.getX(),
                            source.getY(),
                            source.getZ()
                    );
                }
            }

            /*
             * ========================================================
             * 鬼域行为
             * ========================================================
             */

            domain.getBehavior().tick(
                    level,
                    domain
            );

            /*
             * ========================================================
             * 鬼域主人视觉
             * ========================================================
             */

            updateVision(domain);
        }

        /*
         * ============================================================
         * 鬼域主人飞行权限
         * ============================================================
         *
         * 所有鬼域的位置、层数和行为处理完成后，
         * 再统一计算玩家当前是否拥有飞行权限。
         *
         * 这样可以正确处理：
         *
         *   删除最后一个高层鬼域
         *   ↓
         *   本 tick 结束时
         *   ↓
         *   立即取消飞行权限
         */
        for (ServerPlayer player : level.players()) {

            updateOwnerFlight(player);
        }
    }

    /**
     * ============================================================
     * 鬼域主人飞行权限
     * ============================================================
     *
     * <p>
     * 当玩家拥有至少一个达到指定层数的鬼域时，
     * 解锁 Minecraft 原版创造模式飞行。
     *
     * <p>
     * 这里只维护鬼域系统自己授予的飞行权限。
     * 玩家原本因为创造模式、旁观模式或其他系统
     * 获得的飞行权限不会被鬼域系统错误取消。
     *
     * <p>
     * 玩家进入 / 退出飞行仍然由 Minecraft 原版
     * 的“双击空格”机制负责。
     *
     * @param player 鬼域主人
     */
    private void updateOwnerFlight(
            ServerPlayer player
    ) {

        boolean canFly = false;

        /*
         * ========================================================
         * 检查玩家是否拥有能够解锁飞行的鬼域
         * ========================================================
         */

        for (GhostDomain domain : domains.values()) {

            if (!player.getUUID().equals(
                    domain.getSourceUUID()
            )) {
                continue;
            }

            int unlockLayer =
                    domain.getBehavior()
                            .getFlightUnlockLayer();

            /*
             * 解锁层数为 0：
             * 该鬼域不提供飞行能力。
             */
            if (unlockLayer <= 0) {
                continue;
            }

            /*
             * 当前鬼域达到飞行解锁层数。
             */
            if (domain.getLayer() >= unlockLayer) {
                canFly = true;
                break;
            }
        }

        UUID playerUUID =
                player.getUUID();

        /*
         * ========================================================
         * 鬼域授予飞行
         * ========================================================
         */

        if (canFly) {

            /*
             * 玩家已经拥有飞行权限：
             * 不需要修改。
             */
            if (player.getAbilities().mayfly) {
                return;
            }

            /*
             * 玩家原本不能飞，
             * 现在由鬼域系统授予飞行权限。
             */
            player.getAbilities().mayfly = true;

            flightOwners.add(playerUUID);

            /*
             * 同步 Minecraft 原版玩家能力。
             */
            player.onUpdateAbilities();

            return;
        }

        /*
         * ========================================================
         * 鬼域回收飞行
         * ========================================================
         *
         * 只有曾经由鬼域系统授予飞行的玩家，
         * 才允许在这里进行回收。
         */
        if (!flightOwners.remove(playerUUID)) {
            return;
        }

        /*
         * ========================================================
         * 检查 Minecraft 原生飞行权限
         * ========================================================
         *
         * 创造模式和旁观模式本身就拥有飞行能力。
         *
         * 鬼域系统不能因为鬼域消失，
         * 而破坏 Minecraft 原版的飞行能力。
         */
        if (player.isCreative()
                || player.isSpectator()) {

            return;
        }

        /*
         * 生存 / 冒险模式：
         * 真正回收鬼域系统授予的飞行权限。
         */
        player.getAbilities().mayfly = false;

        /*
         * 如果玩家当前正在飞行，
         * 失去鬼域飞行权限后立即退出飞行状态。
         */
        player.getAbilities().flying = false;

        /*
         * 同步 Minecraft 原版玩家能力。
         */
        player.onUpdateAbilities();
    }


    /**
     * 更新指定鬼域主人的视觉状态。
     *
     * <p>
     * 只有鬼域来源实体是玩家时，
     * 才会向对应客户端发送视觉数据。
     *
     * @param domain 鬼域
     */
    private void updateVision(
            GhostDomain domain
    ) {

        if (!domain.getBehavior().hasEntityVision(
                level,
                domain
        )) {

            return;
        }

        Entity source =
                level.getEntity(
                        domain.getSourceUUID()
                );

        /*
         * 当前只有玩家拥有客户端，
         * 因此非玩家来源的鬼域不需要视觉同步。
         */
        if (!(source instanceof ServerPlayer player)) {
            return;
        }

        /*
         * 鬼域主人已经离开当前世界。
         */
        if (!player.isAlive()
                || player.isRemoved()) {
            return;
        }

        Map<UUID, Integer> visibleEntities =
                GhostDomainVisionSystem
                        .collectVisibleEntities(
                                level,
                                domain
                        );

        GhostDomainNetwork.sendVision(
                player,
                domain,
                visibleEntities
        );
    }


    public GhostDomain getOwnEffectiveDomain(
            ServerPlayer player
    ) {

        GhostDomain effectiveDomain = null;

        for (GhostDomain domain : domains.values()) {

            if (!player.getUUID().equals(
                    domain.getSourceUUID()
            )) {
                continue;
            }

            if (!domain.contains(
                    player.getX(),
                    player.getY(),
                    player.getZ()
            )) {
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
}