package com.qidate.qisplan2.ghost.domain.client;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientGhostDomainManager {

    private static final Map<UUID, ClientGhostDomain> DOMAINS =
            new LinkedHashMap<>();

    private static ResourceLocation LAST_DIMENSION;

    private ClientGhostDomainManager() {
    }

    public static void tick(
            ClientTickEvent.Post event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        ResourceLocation currentDimension =
                minecraft.level
                        .dimension()
                        .location();

        /*
         * ========================================================
         * 检测客户端是否切换了维度
         * ========================================================
         */
        if (!currentDimension.equals(LAST_DIMENSION)) {

            LAST_DIMENSION = currentDimension;

            /*
             * 客户端之前保存的 GhostDomain
             * 属于旧维度。
             *
             * 这些域不会收到旧维度广播的 REMOVE，
             * 因此这里必须主动清理。
             */
            clear();

            QisPlan2.LOGGER.info(
                    "[GhostDomain] CLIENT 维度切换，清理旧鬼域: {}",
                    currentDimension
            );
        }

        for (ClientGhostDomain domain : DOMAINS.values()) {
            domain.tick();
        }

        DOMAINS.values().removeIf(
                ClientGhostDomain::isAnimationFinished
        );
    }

    public static void add(
            UUID id,
            UUID sourceUUID,
            ResourceLocation domainType,
            ResourceLocation dimension,
            double x,
            double y,
            double z,
            double strength,
            int layer,
            int shapeType,
            double radius
    ) {

        ClientGhostDomain domain =
                new ClientGhostDomain(
                        id,
                        sourceUUID,
                        domainType,
                        dimension,
                        x,
                        y,
                        z,
                        strength,
                        layer,
                        shapeType,
                        radius
                );

        DOMAINS.put(id, domain);
    }

    public static void remove(UUID id) {

        ClientGhostDomain domain =
                DOMAINS.get(id);

        if (domain == null) {
            return;
        }

        domain.startClosing();
    }

    public static ClientGhostDomain get(UUID id) {
        return DOMAINS.get(id);
    }

    public static Collection<ClientGhostDomain> getDomains() {
        return Collections.unmodifiableCollection(
                DOMAINS.values()
        );
    }

    public static boolean isPositionInsideDomain(
            double x,
            double y,
            double z
    ) {

        ResourceLocation currentDimension =
                getCurrentDimension();

        if (currentDimension == null) {
            return false;
        }

        for (ClientGhostDomain domain : DOMAINS.values()) {

            if (!domain.getDimension().equals(currentDimension)) {
                continue;
            }

            if (domain.contains(x, y, z)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isInsideDomain() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return false;
        }

        return isPositionInsideDomain(
                minecraft.player.getX(),
                minecraft.player.getY(),
                minecraft.player.getZ()
        );
    }

    public static void clear() {
        DOMAINS.clear();
    }

    private static ResourceLocation getCurrentDimension() {

        Minecraft minecraft =
                Minecraft.getInstance();

        ClientLevel level =
                minecraft.level;

        if (level == null) {
            return null;
        }

        return level.dimension().location();
    }

    public static void updatePosition(
            UUID id,
            double x,
            double y,
            double z
    ) {
        ClientGhostDomain domain = DOMAINS.get(id);

        if (domain == null) {
            return;
        }

        domain.setPosition(x, y, z);
    }

    public static void update(
            UUID id,
            double x,
            double y,
            double z,
            double strength,
            int layer,
            double radius,
            boolean immediate
    ) {
        ClientGhostDomain domain = DOMAINS.get(id);

        if (domain == null) {
            return;
        }

        if (immediate) {
            domain.setPositionImmediate(
                    x,
                    y,
                    z
            );
        } else {
            domain.setPosition(
                    x,
                    y,
                    z
            );
        }

        domain.setStrength(strength);
        domain.setLayer(layer);
        domain.setRadius(radius);
    }

    /**
     * 更新指定 GhostDomain 的 Source 所在灵异隔绝 Region。
     *
     * <p>
     * Region UUID 由服务端统一确定，
     * 客户端这里只负责将其保存到对应的
     * {@link ClientGhostDomain} 中。
     *
     * <p>
     * {@code null} 表示 Source 当前位于开放空间。
     *
     * <p>
     * 注意：
     * 这里保存的是 Region 的真实 UUID，
     * 而不是客户端 GPU 使用的临时 Region Index。
     *
     * @param domainId 需要更新的 GhostDomain UUID
     * @param regionId Source 当前所在的 Region UUID；
     *                 如果位于开放空间则为 null
     */
    public static void updateSourceRegion(
            UUID domainId,
            UUID regionId
    ) {

        ClientGhostDomain domain =
                DOMAINS.get(domainId);

        /*
         * ========================================================
         * 客户端当前没有找到对应的 GhostDomain。
         *
         * 这种情况是允许出现的。
         *
         * 例如：
         *
         *     领域数据包还没有到达客户端，
         *     但 Region 更新数据包已经先到达。
         *
         * 当前不创建 GhostDomain，
         * 也不缓存 Region 数据。
         * ========================================================
         */
        if (domain == null) {
            return;
        }

        /*
         * ========================================================
         * 更新 Source 当前所在的 Region。
         * ========================================================
         */
        domain.setSourceRegionUUID(
                regionId
        );
    }

    public static ClientGhostDomain getEffectiveDomain(
            double x,
            double y,
            double z
    ) {

        ClientGhostDomain effective = null;

        for (ClientGhostDomain domain : DOMAINS.values()) {

            /*
             * --------------------------------------------------------
             * 是否处于这个鬼域
             * --------------------------------------------------------
             */

            if (!domain.getShape().contains(
                    domain.getX(),
                    domain.getY(),
                    domain.getZ(),
                    x,
                    y,
                    z
            )) {
                continue;
            }

            /*
             * --------------------------------------------------------
             * 第一个符合条件的鬼域
             * --------------------------------------------------------
             */

            if (effective == null) {
                effective = domain;
                continue;
            }

            /*
             * --------------------------------------------------------
             * 比较鬼域优先级
             * --------------------------------------------------------
             */

            // 更高层级优先
            if (domain.getLayer() > effective.getLayer()) {
                effective = domain;
                continue;
            }

            // 同层级，强度更高优先
            if (domain.getLayer() == effective.getLayer()
                    && domain.getStrength() > effective.getStrength()) {

                effective = domain;
            }
        }

        return effective;
    }

    public static ClientGhostDomain getFirstDomainByType(
            ResourceLocation domainType
    ) {

        ResourceLocation currentDimension =
                getCurrentDimension();

        if (currentDimension == null) {
            return null;
        }

        for (ClientGhostDomain domain : DOMAINS.values()) {

            if (!domain.getDimension().equals(currentDimension)) {
                continue;
            }

            if (!domainType.equals(domain.getDomainType())) {
                continue;
            }

            return domain;
        }

        return null;
    }
}