package com.qidate.qisplan2.ghost.isolation.client;

/**
 * 客户端侧的灵异隔绝空间长方体。
 *
 * <p>
 * 服务端会将一个不规则的灵异隔绝空间
 * 分解成多个轴对齐的长方体。
 * </p>
 *
 * <p>
 * 客户端无需保存每一个被检测过的方块，
 * 只需要保存这些长方体的边界即可。
 * </p>
 */
public record ClientGhostIsolationCuboid(
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
) {

    /**
     * 判断指定的方块坐标是否位于这个长方体内部。
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return 如果坐标位于长方体内部则返回 true
     */
    public boolean contains(
            int x,
            int y,
            int z
    ) {
        return x >= minX
                && x <= maxX
                && y >= minY
                && y <= maxY
                && z >= minZ
                && z <= maxZ;
    }
}