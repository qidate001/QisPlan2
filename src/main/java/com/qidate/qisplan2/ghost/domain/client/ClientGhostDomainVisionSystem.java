package com.qidate.qisplan2.ghost.domain.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 管理客户端当前的鬼域视觉状态。
 *
 * <p>
 * 服务端会按照 GhostDomain 分别同步：
 *
 * <pre>
 *     GhostDomain A → 实体视觉状态
 *     GhostDomain B → 实体视觉状态
 *     GhostDomain C → 实体视觉状态
 * </pre>
 *
 * <p>
 * 客户端分别保存每个鬼域的视觉状态，
 * 然后将所有鬼域的视觉状态合并成最终视觉结果。
 *
 * <p>
 * 本类只负责保存客户端视觉数据，
 * 不负责实际的实体渲染。
 */
public final class ClientGhostDomainVisionSystem {

    /**
     * 每个鬼域自己的视觉状态。
     *
     * <p>
     * Key：
     * GhostDomain UUID
     *
     * <p>
     * Value：
     * Entity UUID → RGB Color
     */
    private static final Map<
            UUID,
            Map<UUID, Integer>
            > DOMAIN_VISION_STATES =
            new LinkedHashMap<>();

    /**
     * 当前最终需要描边的实体。
     *
     * <p>
     * 这是所有鬼域视觉状态合并后的结果。
     *
     * <p>
     * key = 实体 UUID
     * value = RGB 颜色
     */
    private static final Map<UUID, Integer> VISIBLE_ENTITIES =
            new LinkedHashMap<>();

    private ClientGhostDomainVisionSystem() {
    }

    /**
     * 更新指定鬼域的完整视觉状态。
     *
     * <p>
     * 服务端发送的是指定鬼域当前完整的视觉列表。
     * 因此这里只替换该鬼域自己的缓存，
     * 不影响其他鬼域。
     *
     * @param domainId 鬼域 UUID
     * @param entities 实体 UUID → 描边颜色
     */
    public static void update(
            UUID domainId,
            Map<UUID, Integer> entities
    ) {

        DOMAIN_VISION_STATES.put(
                domainId,
                new LinkedHashMap<>(entities)
        );

        rebuildVisibleEntities();
    }

    /**
     * 清除指定鬼域的视觉状态。
     *
     * <p>
     * 当服务端发送一个空视觉列表时，
     * 表示该鬼域当前不再让任何实体产生视觉效果。
     *
     * @param domainId 鬼域 UUID
     */
    public static void clearDomain(
            UUID domainId
    ) {

        DOMAIN_VISION_STATES.remove(
                domainId
        );

        rebuildVisibleEntities();
    }

    /**
     * 清除所有视觉数据。
     *
     * <p>
     * 主要用于客户端切换维度、
     * 重新连接服务器等需要彻底清空状态的情况。
     */
    public static void clear() {

        DOMAIN_VISION_STATES.clear();
        VISIBLE_ENTITIES.clear();
    }

    /**
     * 重新构建最终视觉状态。
     *
     * <p>
     * 将所有鬼域当前的视觉状态合并到
     * {@link #VISIBLE_ENTITIES}。
     */
    private static void rebuildVisibleEntities() {

        VISIBLE_ENTITIES.clear();

        for (Map<UUID, Integer> domainState :
                DOMAIN_VISION_STATES.values()) {

            for (Map.Entry<UUID, Integer> entry :
                    domainState.entrySet()) {

                VISIBLE_ENTITIES.put(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }
    }

    /**
     * 判断实体当前是否需要描边。
     *
     * @param entityUUID 实体 UUID
     * @return 是否需要描边
     */
    public static boolean shouldOutline(
            UUID entityUUID
    ) {

        return VISIBLE_ENTITIES.containsKey(
                entityUUID
        );
    }

    /**
     * 获取实体当前描边颜色。
     *
     * @param entityUUID 实体 UUID
     * @return RGB 颜色；不存在时返回 null
     */
    public static Integer getColor(
            UUID entityUUID
    ) {

        return VISIBLE_ENTITIES.get(
                entityUUID
        );
    }

    /**
     * 获取当前所有描边实体。
     *
     * @return 不可修改的视觉状态
     */
    public static Map<UUID, Integer> getVisibleEntities() {

        return Collections.unmodifiableMap(
                VISIBLE_ENTITIES
        );
    }
}