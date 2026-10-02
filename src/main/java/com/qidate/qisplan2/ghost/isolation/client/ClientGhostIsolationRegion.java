package com.qidate.qisplan2.ghost.isolation.client;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/**
 * 客户端侧的灵异隔绝区域。
 *
 * <p>
 * 一个 Region 由唯一 UUID 标识，
 * 并由一个或多个长方体共同描述其实际的隔绝空间。
 * </p>
 *
 * <p>
 * 客户端保存的是服务端已经确认过的结果，
 * 不负责重新进行灵异隔绝空间检测。
 * </p>
 */
public final class ClientGhostIsolationRegion {

    private final UUID id;

    private final ResourceLocation dimension;

    private final List<ClientGhostIsolationCuboid> cuboids;

    /**
     * 创建一个客户端灵异隔绝区域。
     *
     * @param id 区域 UUID
     * @param dimension 区域所在维度
     * @param cuboids 区域包含的精确长方体
     */
    public ClientGhostIsolationRegion(
            UUID id,
            ResourceLocation dimension,
            List<ClientGhostIsolationCuboid> cuboids
    ) {
        this.id = id;
        this.dimension = dimension;
        this.cuboids = List.copyOf(cuboids);
    }

    /**
     * 获取区域 UUID。
     *
     * @return 区域 UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * 获取区域所在维度。
     *
     * @return 维度 ID
     */
    public ResourceLocation getDimension() {
        return dimension;
    }

    /**
     * 获取区域包含的所有长方体。
     *
     * @return 不可修改的长方体列表
     */
    public List<ClientGhostIsolationCuboid> getCuboids() {
        return cuboids;
    }

    /**
     * 判断指定坐标是否位于这个灵异隔绝区域内。
     *
     * <p>
     * 这里直接遍历精确长方体。
     * 后续如果客户端 Region 数量增加，
     * 再增加空间索引进行优化。
     * </p>
     *
     * @param dimension 当前所在维度
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return 如果坐标位于隔绝区域内则返回 true
     */
    public boolean contains(
            ResourceLocation dimension,
            int x,
            int y,
            int z
    ) {
        if (!this.dimension.equals(dimension)) {
            return false;
        }

        for (ClientGhostIsolationCuboid cuboid :
                cuboids) {

            if (cuboid.contains(
                    x,
                    y,
                    z
            )) {
                return true;
            }
        }

        return false;
    }
}