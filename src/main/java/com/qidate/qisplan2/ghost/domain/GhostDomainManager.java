package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationSystem;
import com.qidate.qisplan2.network.QisNetwork;
import com.qidate.qisplan2.network.ghostdomain.GhostDomainNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.*;

public final class GhostDomainManager {

    private static final Map<ServerLevel, GhostDomainManager>
            MANAGERS = new WeakHashMap<>();

    private final ServerLevel level;

    private final Map<UUID, GhostDomain> domains =
            new LinkedHashMap<>();

    private final Map<UUID, Integer> behaviorTickCounters =
            new HashMap<>();

    /**
     * 鬼域视觉同步缓存。
     *
     * <p>
     * Key：
     *     GhostDomain UUID
     *
     * <p>
     * Value：
     *     上一次已经发送给鬼域主人的视觉状态。
     *
     * <p>
     * 视觉状态：
     *
     *     Entity UUID → RGB Color
     *
     * <p>
     * 只有视觉状态发生变化时，
     * 才重新发送 GhostDomainVisionPayload。
     */
    private final Map<UUID, Map<UUID, Integer>> visionStates =
            new HashMap<>();

    /**
     * 每个灵异领域 Source 当前所在的灵异隔绝 Region。
     *
     * <p>
     * Key：
     *     GhostDomain 的 UUID
     *
     * <p>
     * Value：
     *     Source 当前所在的 Region UUID。
     *
     * <p>
     * 如果 Source 当前位于开放空间，
     * 或者没有处于任何有效的灵异隔绝 Region，
     * 则对应的 Value 为 {@code null}。
     *
     * <p>
     * 这里保存的是 Region 的真实 UUID，
     * 而不是客户端 GPU 使用的临时 Region Index。
     */
    private final Map<UUID, UUID> sourceRegionIds =
            new HashMap<>();

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

        GhostDomainEntityTracker.get(level)
                .updateDomain(domain);

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

        /*
         * ========================================================
         * 获取鬼域
         * ========================================================
         */
        GhostDomain domain =
                domains.remove(id);

        if (domain == null) {
            return;
        }

        /*
         * ========================================================
         * 清理 Source Region 缓存
         * ========================================================
         */
        sourceRegionIds.remove(
                domain.getId()
        );

        /*
         * ========================================================
         * 清理鬼域视觉同步缓存
         * ========================================================
         */
        visionStates.remove(
                domain.getId()
        );

        /*
         * ========================================================
         * 清理 Behavior 周期计时器
         * ========================================================
         *
         * 该鬼域已经被删除，
         * 不再需要保存它的周期执行状态。
         */
        behaviorTickCounters.remove(
                domain.getId()
        );

        /*
         * ========================================================
         * 清理实体追踪状态
         * ========================================================
         */
        GhostDomainEntityTracker
                .get(level)
                .removeDomain(domain);

        /*
         * ========================================================
         * 鬼域行为收尾
         * ========================================================
         *
         * 所有具体鬼域自己的清理逻辑，
         * 统一从这里进入。
         */
        domain.getBehavior().onRemove(
                level,
                domain
        );

        /*
         * ========================================================
         * 清理鬼域主人视觉
         * ========================================================
         */
        Entity source =
                level.getEntity(
                        domain.getSourceUUID()
                );

        if (source instanceof ServerPlayer player) {

            GhostDomainNetwork.sendVision(
                    player,
                    domain,
                    Map.of()
            );
        }

        /*
         * ========================================================
         * 通知客户端删除鬼域
         * ========================================================
         */
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

            AABB oldArea =
                    createDomainAABB(domain);

            domain.setPosition(x, y, z);

            GhostDomainEntityTracker.get(level)
                    .updateDomain(
                            domain,
                            oldArea
                    );

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

        AABB oldArea =
                createDomainAABB(domain);

        domain.setPosition(x, y, z);

        GhostDomainEntityTracker.get(level)
                .updateDomain(
                        domain,
                        oldArea
                );

        GhostDomainNetwork.sendUpdate(
                level,
                domain,
                false
        );
    }

    private AABB createDomainAABB(
            GhostDomain domain
    ) {
        double radius =
                domain.getRadius();

        return new AABB(
                domain.getX() - radius,
                domain.getY() - radius,
                domain.getZ() - radius,
                domain.getX() + radius,
                domain.getY() + radius,
                domain.getZ() + radius
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

        AABB oldArea =
                createDomainAABB(domain);

        domain.setPosition(
                x,
                y,
                z
        );

        GhostDomainEntityTracker
                .get(level)
                .updateDomain(
                        domain,
                        oldArea
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

    /**
     * ========================================
     * 同步鬼域运行时状态
     * ========================================
     *
     * 用于 Domain 的强度、半径等运行时状态发生变化后，
     * 主动通知客户端。
     */
    public void syncUpdate(
            GhostDomain domain
    ) {
        if (!domains.containsKey(domain.getId())) {
            return;
        }

        GhostDomainNetwork.sendUpdate(
                level,
                domain,
                true
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

    /**
     * 更新指定灵异领域 Source 当前所在的灵异隔绝 Region。
     *
     * <p>
     * 本方法只负责检测 Source 的 Region 身份变化，
     * 当前阶段暂不进行网络同步。
     *
     * <p>
     * Region 身份使用 {@link UUID} 表示，
     * 而不是客户端 GPU 使用的临时 Region Index。
     *
     * <p>
     * 只有以下情况发生变化时，
     * 才会更新缓存：
     *
     * <ul>
     *     <li>开放空间 → Region</li>
     *     <li>Region → 开放空间</li>
     *     <li>Region A → Region B</li>
     * </ul>
     *
     * @param domain 需要检测的灵异领域
     */
    private void updateSourceRegion(
            GhostDomain domain
    ) {

        Entity source =
                level.getEntity(
                        domain.getSourceUUID()
                );

        /*
         * ========================================================
         * Source 不存在。
         *
         * 当前不主动改变缓存。
         * ========================================================
         */
        if (source == null) {
            return;
        }

        UUID currentRegionId =
                GhostIsolationSystem.getRegionId(
                        level,
                        source.blockPosition()
                );

        UUID previousRegionId =
                sourceRegionIds.get(
                        domain.getId()
                );

        /*
         * ========================================================
         * Region 身份没有发生变化。
         *
         * 不需要进行任何处理。
         * ========================================================
         */
        if (Objects.equals(
                previousRegionId,
                currentRegionId
        )) {
            return;
        }

        /*
         * ========================================================
         * Source 的 Region 身份发生变化。
         *
         * 更新服务端缓存。
         * ========================================================
         */
        sourceRegionIds.put(
                domain.getId(),
                currentRegionId
        );

        /*
         * ========================================================
         * 将新的 Region 身份同步给客户端。
         *
         * 这里只在 Region 身份发生变化时发送，
         * 不会因为 GhostDomain 每 tick 更新
         * 而重复发送相同的数据。
         * ========================================================
         */
        GhostDomainNetwork.sendSourceRegion(
                level,
                domain,
                currentRegionId
        );

        QisPlan2.LOGGER.info(
                "[GhostDomain] Source 所在灵异隔绝 Region 发生变化：domain={}, source={}, {} -> {}",
                domain.getId(),
                domain.getSourceUUID(),
                previousRegionId,
                currentRegionId
        );
    }

    public void tick() {

        /*
         * ============================================================
         * 使用快照遍历
         * ============================================================
         *
         * 因为本次 tick 可能会发现 Source 已经不存在，
         * 从而调用 remove() 修改 domains。
         *
         * 直接遍历 domains.values() 会导致
         * ConcurrentModificationException。
         */
        for (GhostDomain domain :
                new ArrayList<>(domains.values())) {

            /*
             * ========================================================
             * 检查 Source 是否仍然存在
             * ========================================================
             */
            Entity source =
                    level.getEntity(
                            domain.getSourceUUID()
                    );

            if (source == null
                    || source.isRemoved()) {

                /*
                 * Source 已经不存在。
                 *
                 * 由 GhostDomainManager 统一完成
                 * 整个鬼域的生命周期收尾。
                 */
                remove(
                        domain.getId()
                );

                continue;
            }

            /*
             * ========================================================
             * 自动更新鬼域位置
             * ========================================================
             */
            if (domain.getUpdateMode()
                    == GhostDomainUpdateMode.DISTANCE) {

                updatePosition(
                        domain.getId(),
                        source.getX(),
                        source.getY(),
                        source.getZ()
                );
            }

            /*
             * ========================================================
             * Source 所在灵异隔绝区域
             * ========================================================
             */
            updateSourceRegion(domain);

            /*
             * ========================================================
             * Behavior 周期行为
             * ========================================================
             */
            tickBehavior(domain);

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
         */
        for (ServerPlayer player : level.players()) {

            updateOwnerFlight(
                    player
            );
        }
    }

    /**
     * 执行鬼域行为的周期性逻辑。
     *
     * <p>
     * GhostDomainManager 负责统一控制
     * 行为的执行频率，而 GhostDomainBehavior
     * 不再自行实现周期性 Tick 调度。
     * </p>
     *
     * <p>
     * 实体关系由 GhostDomainEntityTracker
     * 统一维护。
     *
     * <p>
     * 这里向 Behavior 提供的是：
     *
     * <ul>
     *     <li>当前仍然存在的实体</li>
     *     <li>当前最终生效于该鬼域的实体</li>
     * </ul>
     *
     * <p>
     * Behavior 不需要再次访问 Tracker，
     * 也不需要重新判断鬼域覆盖关系。
     * </p>
     *
     * @param domain 当前鬼域
     */
    private void tickBehavior(
            GhostDomain domain
    ) {

        GhostDomainBehavior behavior =
                domain.getBehavior();

        int interval =
                behavior.getTickInterval();

        if (interval <= 0) {
            return;
        }

        int tick =
                behaviorTickCounters.merge(
                        domain.getId(),
                        1,
                        Integer::sum
                );

        if (tick < interval) {
            return;
        }

        behaviorTickCounters.put(
                domain.getId(),
                0
        );

        /*
         * ========================================
         * 获取当前最终生效实体
         * ========================================
         *
         * Tracker 已经完成：
         *
         * 1. 空间关系维护
         * 2. 鬼域覆盖关系维护
         * 3. 鬼域优先级计算
         * 4. 最终生效鬼域计算
         *
         * Behavior 不再参与这些工作。
         */
        Set<Entity> entities =
                GhostDomainEntityTracker
                        .get(level)
                        .getEffectiveEntities(domain);

        /*
         * ========================================
         * 执行鬼域行为
         * ========================================
         */
        behavior.onTick(
                level,
                domain,
                entities
        );
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
     * <p>
     * 视觉数据只有在发生变化时才会发送。
     *
     * @param domain 鬼域
     */
    private void updateVision(
            GhostDomain domain
    ) {

        /*
         * ========================================================
         * 检查鬼域是否拥有实体视觉
         * ========================================================
         */
        if (!domain.getBehavior().hasEntityVision(
                level,
                domain
        )) {

            /*
             * 当前鬼域不使用实体视觉。
             *
             * 如果之前存在视觉缓存，
             * 这里也应该清掉。
             */
            visionStates.remove(
                    domain.getId()
            );

            return;
        }

        /*
         * ========================================================
         * 获取鬼域主人
         * ========================================================
         */
        Entity source =
                level.getEntity(
                        domain.getSourceUUID()
                );

        /*
         * 当前只有玩家拥有客户端。
         *
         * 非玩家来源的鬼域不需要视觉同步。
         */
        if (!(source instanceof ServerPlayer player)) {

            visionStates.remove(
                    domain.getId()
            );

            return;
        }

        /*
         * ========================================================
         * 检查玩家状态
         * ========================================================
         */
        if (!player.isAlive()
                || player.isRemoved()) {

            visionStates.remove(
                    domain.getId()
            );

            return;
        }

        /*
         * ========================================================
         * 计算当前视觉状态
         * ========================================================
         */
        Map<UUID, Integer> visibleEntities =
                GhostDomainVisionSystem
                        .collectVisibleEntities(
                                level,
                                domain
                        );

        /*
         * ========================================================
         * 获取上一次已经同步的状态
         * ========================================================
         */
        Map<UUID, Integer> previousState =
                visionStates.get(
                        domain.getId()
                );

        /*
         * ========================================================
         * 视觉状态没有发生变化
         * ========================================================
         *
         * 第一次同步时 previousState == null，
         * 因此第一次一定会发送。
         */
        if (previousState != null
                && previousState.equals(
                visibleEntities
        )) {

            return;
        }

        /*
         * ========================================================
         * 视觉状态发生变化
         * ========================================================
         *
         * 现在才真正发送网络包。
         */
        GhostDomainNetwork.sendVision(
                player,
                domain,
                visibleEntities
        );

        /*
         * ========================================================
         * 保存本次已经成功同步的状态
         * ========================================================
         *
         * 必须复制 Map。
         *
         * 不能直接保存 visibleEntities 的引用，
         * 避免未来代码修改这个 Map 时影响缓存。
         */
        visionStates.put(
                domain.getId(),
                new LinkedHashMap<>(
                        visibleEntities
                )
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