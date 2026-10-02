package com.qidate.qisplan2.ghost.isolation.client.renderer;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationCuboid;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationManager;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationRegion;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 灵异隔绝 Cuboid 的 GPU 数据管理器。
 *
 * <p>
 * 当前阶段负责将客户端的所有灵异隔绝 Cuboid
 * 展平为连续的 GPU 数据。
 *
 * <p>
 * GPU 数据纹理的具体 OpenGL 实现暂时独立出来，
 * 防止客户端 Region 数据层和 OpenGL 生命周期耦合。
 */
public final class GhostIsolationGpuData {

    /**
     * GPU 最多保存的 Cuboid 数量。
     */
    public static final int MAX_CUBOIDS = 256;

    /**
     * 当前 Cuboid 数量。
     */
    private static int cuboidCount = 0;

    /**
     * Cuboid 最小坐标。
     *
     * <p>
     * 每三个 float 表示一个 Cuboid：
     *
     * <pre>
     * [x, y, z]
     * [x, y, z]
     * ...
     * </pre>
     */
    private static final float[] CUBOID_MINS =
            new float[MAX_CUBOIDS * 3];

    /**
     * Cuboid 最大坐标。
     *
     * <p>
     * 注意这里保存的是：
     *
     * <pre>
     * max + 1
     * </pre>
     *
     * 从而与 Shader 中的半开区间：
     *
     * <pre>
     * min <= position < max
     * </pre>
     *
     * 保持一致。
     */
    private static final float[] CUBOID_MAXS =
            new float[MAX_CUBOIDS * 3];

    /**
     * 当前 GPU 数据是否需要重新上传。
     */
    private static boolean dirty = true;

    private GhostIsolationGpuData() {
    }

    /**
     * 根据客户端当前的灵异隔绝区域，
     * 重建 GPU 数据。
     *
     * <p>
     * 这个方法运行在客户端线程。
     */
    public static void rebuild() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            clear();
            return;
        }

        clearData();

        int count = 0;

        /*
         * ====================================================
         * 遍历客户端当前保存的 Region
         * ====================================================
         */
        for (ClientGhostIsolationRegion region :
                ClientGhostIsolationManager.getRegions()) {

            /*
             * 只处理当前维度。
             */
            if (!region.getDimension().equals(
                    minecraft.level.dimension().location()
            )) {
                continue;
            }

            /*
             * ====================================================
             * 展开 Region 中的全部 Cuboid
             * ====================================================
             */
            for (ClientGhostIsolationCuboid cuboid :
                    region.getCuboids()) {

                if (count >= MAX_CUBOIDS) {

                    QisPlan2.LOGGER.warn(
                            "[灵异隔绝 GPU] Cuboid 数量超过上限 {}，"
                                    + "后续 Cuboid 将被忽略。",
                            MAX_CUBOIDS
                    );

                    cuboidCount = count;
                    dirty = true;
                    return;
                }

                int offset = count * 3;

                /*
                 * =================================================
                 * Min
                 * =================================================
                 */
                CUBOID_MINS[offset] =
                        cuboid.minX();

                CUBOID_MINS[offset + 1] =
                        cuboid.minY();

                CUBOID_MINS[offset + 2] =
                        cuboid.minZ();

                /*
                 * =================================================
                 * Max
                 *
                 * Minecraft Cuboid 是包含 max 的。
                 *
                 * Shader 使用：
                 *
                 *     min <= position < max
                 *
                 * 所以这里必须 +1。
                 * =================================================
                 */
                CUBOID_MAXS[offset] =
                        cuboid.maxX() + 1.0F;

                CUBOID_MAXS[offset + 1] =
                        cuboid.maxY() + 1.0F;

                CUBOID_MAXS[offset + 2] =
                        cuboid.maxZ() + 1.0F;

                count++;
            }
        }

        cuboidCount = count;

        dirty = true;

        QisPlan2.LOGGER.info(
                "[灵异隔绝 GPU] 重建数据：Cuboid={}",
                cuboidCount
        );
    }

    /**
     * 清空所有 Cuboid 数据。
     */
    private static void clearData() {

        java.util.Arrays.fill(
                CUBOID_MINS,
                0.0F
        );

        java.util.Arrays.fill(
                CUBOID_MAXS,
                0.0F
        );

        cuboidCount = 0;
    }

    /**
     * 清空客户端 GPU 数据状态。
     */
    public static void clear() {

        clearData();

        dirty = true;

        QisPlan2.LOGGER.info(
                "[灵异隔绝 GPU] 数据已清空"
        );
    }

    /**
     * 当前是否有 Cuboid 数据。
     */
    public static boolean hasData() {
        return cuboidCount > 0;
    }

    /**
     * 获取当前 Cuboid 数量。
     */
    public static int getCuboidCount() {
        return cuboidCount;
    }

    /**
     * 获取 Cuboid Min 数据。
     *
     * <p>
     * 仅供 GPU 上传阶段使用。
     */
    public static float[] getCuboidMins() {
        return CUBOID_MINS;
    }

    /**
     * 获取 Cuboid Max 数据。
     *
     * <p>
     * 仅供 GPU 上传阶段使用。
     */
    public static float[] getCuboidMaxs() {
        return CUBOID_MAXS;
    }

    /**
     * GPU 数据是否发生变化。
     */
    public static boolean isDirty() {
        return dirty;
    }

    /**
     * 标记 GPU 数据已经上传。
     */
    public static void markUploaded() {
        dirty = false;
    }

    /**
     * 获取指定 Cuboid 的 Min X/Y/Z。
     */
    public static float getMinX(int index) {
        return CUBOID_MINS[index * 3];
    }

    public static float getMinY(int index) {
        return CUBOID_MINS[index * 3 + 1];
    }

    public static float getMinZ(int index) {
        return CUBOID_MINS[index * 3 + 2];
    }

    /**
     * 获取指定 Cuboid 的 Max X/Y/Z。
     */
    public static float getMaxX(int index) {
        return CUBOID_MAXS[index * 3];
    }

    public static float getMaxY(int index) {
        return CUBOID_MAXS[index * 3 + 1];
    }

    public static float getMaxZ(int index) {
        return CUBOID_MAXS[index * 3 + 2];
    }
}