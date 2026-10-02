package com.qidate.qisplan2.ghost.isolation.client;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端灵异隔绝区域管理器。
 *
 * <p>
 * 负责保存服务端同步到客户端的灵异隔绝区域。
 * </p>
 *
 * <p>
 * 客户端只保存服务端已经确认的结果，
 * 不在这里进行空间检测或重新计算。
 * </p>
 */
public final class ClientGhostIsolationManager {

    private static final Map<UUID, ClientGhostIsolationRegion> REGIONS =
            new LinkedHashMap<>();

    private ClientGhostIsolationManager() {
    }

    /**
     * 添加或覆盖一个灵异隔绝区域。
     *
     * <p>
     * 如果客户端已经存在相同 UUID 的区域，
     * 新数据会直接覆盖旧数据。
     * </p>
     *
     * @param id 区域 UUID
     * @param dimension 区域所在维度
     * @param cuboids 区域包含的长方体
     */
    public static void add(
            UUID id,
            ResourceLocation dimension,
            List<ClientGhostIsolationCuboid> cuboids
    ) {
        REGIONS.put(
                id,
                new ClientGhostIsolationRegion(
                        id,
                        dimension,
                        cuboids
                )
        );

        QisPlan2.LOGGER.info(
                "[灵异隔绝客户端] ADD Region: id={}, dimension={}, cuboids={}",
                id,
                dimension,
                cuboids.size()
        );
    }

    /**
     * 移除指定的灵异隔绝区域。
     *
     * @param id 要移除的区域 UUID
     */
    public static void remove(
            UUID id
    ) {
        REGIONS.remove(id);

        QisPlan2.LOGGER.info(
                "[灵异隔绝客户端] REMOVE Region: {}",
                id
        );
    }

    /**
     * 获取客户端当前保存的所有灵异隔绝区域。
     *
     * @return 不可修改的区域集合
     */
    public static Collection<ClientGhostIsolationRegion> getRegions() {
        return Collections.unmodifiableCollection(
                REGIONS.values()
        );
    }

    /**
     * 获取指定 UUID 的灵异隔绝区域。
     *
     * @param id 区域 UUID
     * @return 对应区域，如果不存在则返回 null
     */
    public static ClientGhostIsolationRegion get(
            UUID id
    ) {
        return REGIONS.get(id);
    }

    /**
     * 判断指定位置是否处于客户端已知的灵异隔绝空间中。
     *
     * @param dimension 当前所在维度
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return 如果当前位置处于灵异隔绝空间中则返回 true
     */
    public static boolean isIsolated(
            ResourceLocation dimension,
            int x,
            int y,
            int z
    ) {
        for (ClientGhostIsolationRegion region :
                REGIONS.values()) {

            if (region.contains(
                    dimension,
                    x,
                    y,
                    z
            )) {
                return true;
            }
        }

        return false;
    }

    /**
     * 清空客户端当前保存的所有灵异隔绝区域。
     *
     * <p>
     * 玩家退出世界、断开服务器或切换维度时，
     * 可以使用此方法清理旧的客户端数据。
     * </p>
     */
    public static void clear() {
        REGIONS.clear();
    }
}