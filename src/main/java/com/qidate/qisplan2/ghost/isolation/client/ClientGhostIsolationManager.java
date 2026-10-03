package com.qidate.qisplan2.ghost.isolation.client;

import com.qidate.qisplan2.ghost.isolation.client.renderer.GhostIsolationGpuData;
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

    /**
     * 隔绝空间 UUID → 当前 GPU 数据中的 Region Index。
     *
     * <p>
     * 该 Index 只是 GPU 数据的临时索引，
     * Region 的真正身份仍然由 UUID 决定。
     */
    private static final Map<UUID, Integer> REGION_INDICES =
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

        rebuildRegionIndices();

        GhostIsolationGpuData.rebuild();
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

        rebuildRegionIndices();

        GhostIsolationGpuData.rebuild();
    }

    /**
     * 根据当前客户端存在的隔绝空间重新建立 GPU Region Index。
     *
     * <p>
     * Index 只服务于当前 GPU 数据，
     * 因此隔绝空间增删后允许重新编号。
     */
    private static void rebuildRegionIndices() {

        REGION_INDICES.clear();

        int index = 0;

        for (UUID regionId : REGIONS.keySet()) {

            REGION_INDICES.put(
                    regionId,
                    index
            );

            index++;
        }
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
     * 获取隔绝空间当前对应的 GPU Region Index。
     *
     * @param regionId 隔绝空间 UUID
     * @return GPU Region Index；不存在时返回 -1
     */
    public static int getRegionIndex(UUID regionId) {

        Integer index = REGION_INDICES.get(regionId);

        if (index == null) {
            return -1;
        }

        return index;
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
     * 获取指定位置所属的灵异隔绝空间。
     *
     * <p>
     * 返回值：
     * <ul>
     *     <li>{@code null}：当前位置属于普通空间</li>
     *     <li>非 {@code null}：当前位置属于对应的隔绝空间</li>
     * </ul>
     *
     * @param dimension 维度
     * @param x 世界 X 坐标
     * @param y 世界 Y 坐标
     * @param z 世界 Z 坐标
     * @return 所属隔绝空间的 UUID；如果不属于任何隔绝空间则返回 null
     */
    public static UUID getRegionId(
            ResourceLocation dimension,
            double x,
            double y,
            double z
    ) {

        for (ClientGhostIsolationRegion region :
                REGIONS.values()) {

            /*
             * 隔绝空间只属于自己的维度。
             */
            if (!region.getDimension().equals(
                    dimension
            )) {
                continue;
            }

            /*
             * 检查当前位置是否位于该隔绝空间。
             */
            if (region.contains(
                    dimension,
                    (int) Math.floor(x),
                    (int) Math.floor(y),
                    (int) Math.floor(z)
            )) {
                return region.getId();
            }
        }

        /*
         * 没有命中任何隔绝空间，
         * 说明当前位置属于普通空间。
         */
        return null;
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

        REGION_INDICES.clear();

        GhostIsolationGpuData.rebuild();
    }
}