package com.qidate.qisplan2.ghost.partition;

/**
 * 划分维度区域管理。
 *
 * 每个逻辑区域：
 *
 * 200 × 200 chunk
 * = 3200 × 3200 block
 */
public final class PartitionSpaceManager {

    /**
     * 一个区域的宽度：
     *
     * 200 chunk。
     */
    public static final int REGION_CHUNKS = 200;

    /**
     * Minecraft 一个 chunk = 16 block。
     */
    public static final int REGION_BLOCKS =
            REGION_CHUNKS * 16;

    /**
     * 默认区域中心 Y。
     */
    public static final int REGION_Y = 65;

    private PartitionSpaceManager() {
    }

    /**
     * 区域 ID → 区域 X。
     *
     * 第一版采用简单的二维网格。
     */
    public static int getRegionX(
            long regionId
    ) {

        int gridSize = 1000;

        return Math.floorMod(
                Math.toIntExact(regionId),
                gridSize
        );
    }

    /**
     * 区域 ID → 区域 Z。
     */
    public static int getRegionZ(
            long regionId
    ) {

        int gridSize = 1000;

        return Math.floorDiv(
                Math.toIntExact(regionId),
                gridSize
        );
    }

    /**
     * 获取区域中心 X。
     */
    public static double getCenterX(
            long regionId
    ) {

        return getRegionX(
                regionId
        ) * REGION_BLOCKS
                + REGION_BLOCKS / 2.0D;
    }

    /**
     * 获取区域中心 Z。
     */
    public static double getCenterZ(
            long regionId
    ) {

        return getRegionZ(
                regionId
        ) * REGION_BLOCKS
                + REGION_BLOCKS / 2.0D;
    }

    /**
     * 获取区域中心 Y。
     */
    public static double getCenterY() {

        return REGION_Y;
    }
}