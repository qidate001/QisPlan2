package com.qidate.qisplan2.ghost.domain.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 管理客户端当前的鬼域视觉状态。
 *
 * <p>
 * 服务端会将鬼域主人应该看到的实体
 * 以及对应的描边颜色同步到这里。
 *
 * <p>
 * 本类只负责保存客户端视觉数据，
 * 不负责实际的实体渲染。
 */
public final class ClientGhostDomainVisionSystem {

    /**
     * 当前所有需要描边的实体。
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
     * 当前服务端发送的是完整列表，
     * 因此收到数据后直接替换当前状态。
     *
     * @param entities 实体 UUID → 描边颜色
     */
    public static void update(
            Map<UUID, Integer> entities
    ) {

        VISIBLE_ENTITIES.clear();

        VISIBLE_ENTITIES.putAll(
                entities
        );
    }

    /**
     * 清除所有视觉数据。
     */
    public static void clear() {

        VISIBLE_ENTITIES.clear();
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