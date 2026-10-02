package com.qidate.qisplan2.ghost.isolation;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将 Flood Fill 得到的离散空间方块，
 * 分解为一组轴对齐 Cuboid。
 *
 * <p>
 * 分解结果必须满足：
 *
 * <pre>
 * 所有 Cuboid 的并集 == 原始 BlockPos 集合
 * </pre>
 *
 * <p>
 * 第一版优先保证正确性，
 * 不追求理论上的最少 Cuboid 数量。
 */
public final class GhostIsolationCuboidDecomposer {

    private GhostIsolationCuboidDecomposer() {
    }

    public static List<GhostIsolationCuboid> decompose(
            Set<BlockPos> blocks
    ) {

        if (blocks.isEmpty()) {
            return List.of();
        }

        /*
         * ========================================================
         * 按 Y 分层。
         * ========================================================
         */
        Map<Integer, Set<BlockPos>> layers =
                new HashMap<>();

        for (BlockPos pos : blocks) {

            layers.computeIfAbsent(
                    pos.getY(),
                    ignored -> new HashSet<>()
            ).add(pos);
        }

        List<Integer> ys =
                new ArrayList<>(
                        layers.keySet()
                );

        ys.sort(Integer::compareTo);

        /*
         * 当前正在跨 Y 延伸的矩形。
         *
         * key：
         *     XZ 平面的矩形
         *
         * value：
         *     这个矩形开始于哪个 Y
         */
        Map<RectangleKey, Integer> active =
                new HashMap<>();

        List<CuboidData> finished =
                new ArrayList<>();

        int previousY =
                Integer.MIN_VALUE;

        /*
         * ========================================================
         * 逐个 Y 层处理。
         * ========================================================
         */
        for (int y : ys) {

            /*
             * Y 不连续，
             * 当前所有 Cuboid 都必须结束。
             */
            if (previousY != Integer.MIN_VALUE
                    && y != previousY + 1) {

                finishActiveCuboids(
                        active,
                        previousY,
                        finished
                );

                active.clear();
            }

            Set<BlockPos> layer =
                    layers.get(y);

            /*
             * 当前层拆成 XZ 矩形。
             */
            List<RectangleKey> rectangles =
                    decomposeLayer(layer);

            Set<RectangleKey> current =
                    new HashSet<>(
                            rectangles
                    );

            /*
             * 当前层已经不存在的矩形，
             * 结束对应 Cuboid。
             */
            List<RectangleKey> toFinish =
                    new ArrayList<>();

            for (RectangleKey rectangle :
                    active.keySet()) {

                if (!current.contains(rectangle)) {
                    toFinish.add(rectangle);
                }
            }

            for (RectangleKey rectangle :
                    toFinish) {

                int minY =
                        active.remove(
                                rectangle
                        );

                finished.add(
                        new CuboidData(
                                rectangle,
                                minY,
                                previousY
                        )
                );
            }

            /*
             * 当前层出现的矩形：
             *
             * 已存在：
             *     继续延伸
             *
             * 不存在：
             *     从当前 Y 开始
             */
            for (RectangleKey rectangle :
                    rectangles) {

                active.putIfAbsent(
                        rectangle,
                        y
                );
            }

            previousY = y;
        }

        /*
         * 最后一层结束所有 Cuboid。
         */
        finishActiveCuboids(
                active,
                previousY,
                finished
        );

        /*
         * ========================================================
         * 转换成真正的 GhostIsolationCuboid。
         * ========================================================
         */
        List<GhostIsolationCuboid> result =
                new ArrayList<>(
                        finished.size()
                );

        for (CuboidData cuboid :
                finished) {

            RectangleKey rectangle =
                    cuboid.rectangle();

            result.add(
                    new GhostIsolationCuboid(
                            new BlockPos(
                                    rectangle.minX(),
                                    cuboid.minY(),
                                    rectangle.minZ()
                            ),
                            new BlockPos(
                                    rectangle.maxX(),
                                    cuboid.maxY(),
                                    rectangle.maxZ()
                            )
                    )
            );
        }

        return result;
    }

    /**
     * 将一个 Y 层拆成 XZ 平面的矩形。
     */
    private static List<RectangleKey> decomposeLayer(
            Set<BlockPos> layer
    ) {

        /*
         * Z → 这一行存在的 X。
         */
        Map<Integer, List<Integer>> rows =
                new HashMap<>();

        for (BlockPos pos : layer) {

            rows.computeIfAbsent(
                    pos.getZ(),
                    ignored -> new ArrayList<>()
            ).add(pos.getX());
        }

        /*
         * 每一行的 X 坐标排序。
         */
        for (List<Integer> xs :
                rows.values()) {

            xs.sort(Integer::compareTo);
        }

        List<Integer> zs =
                new ArrayList<>(
                        rows.keySet()
                );

        zs.sort(Integer::compareTo);

        /*
         * 当前正在延伸的 X 区间。
         */
        Map<XInterval, Integer> active =
                new HashMap<>();

        List<RectangleKey> result =
                new ArrayList<>();

        int previousZ =
                Integer.MIN_VALUE;

        /*
         * ========================================================
         * 逐行处理。
         * ========================================================
         */
        for (int z : zs) {

            /*
             * Z 不连续，
             * 当前所有矩形结束。
             */
            if (previousZ != Integer.MIN_VALUE
                    && z != previousZ + 1) {

                finishActiveRectangles(
                        active,
                        previousZ,
                        result
                );

                active.clear();
            }

            List<XInterval> intervals =
                    buildXIntervals(
                            rows.get(z)
                    );

            Set<XInterval> current =
                    new HashSet<>(
                            intervals
                    );

            /*
             * 当前行不存在的区间结束。
             */
            List<XInterval> toFinish =
                    new ArrayList<>();

            for (XInterval interval :
                    active.keySet()) {

                if (!current.contains(interval)) {
                    toFinish.add(interval);
                }
            }

            for (XInterval interval :
                    toFinish) {

                int minZ =
                        active.remove(
                                interval
                        );

                result.add(
                        new RectangleKey(
                                interval.minX(),
                                interval.maxX(),
                                minZ,
                                previousZ
                        )
                );
            }

            /*
             * 当前行的区间继续延伸，
             * 或者从当前 Z 开始创建。
             */
            for (XInterval interval :
                    intervals) {

                active.putIfAbsent(
                        interval,
                        z
                );
            }

            previousZ = z;
        }

        /*
         * 最后一行结束所有矩形。
         */
        finishActiveRectangles(
                active,
                previousZ,
                result
        );

        return result;
    }

    /**
     * 将一行连续的 X 坐标转换成区间。
     */
    private static List<XInterval> buildXIntervals(
            List<Integer> xs
    ) {

        List<XInterval> result =
                new ArrayList<>();

        if (xs.isEmpty()) {
            return result;
        }

        int minX = xs.get(0);
        int maxX = minX;

        for (int i = 1; i < xs.size(); i++) {

            int x = xs.get(i);

            if (x == maxX + 1) {
                maxX = x;
                continue;
            }

            result.add(
                    new XInterval(
                            minX,
                            maxX
                    )
            );

            minX = x;
            maxX = x;
        }

        result.add(
                new XInterval(
                        minX,
                        maxX
                )
        );

        return result;
    }

    private static void finishActiveRectangles(
            Map<XInterval, Integer> active,
            int maxZ,
            List<RectangleKey> result
    ) {

        for (Map.Entry<XInterval, Integer> entry :
                active.entrySet()) {

            XInterval interval =
                    entry.getKey();

            int minZ =
                    entry.getValue();

            result.add(
                    new RectangleKey(
                            interval.minX(),
                            interval.maxX(),
                            minZ,
                            maxZ
                    )
            );
        }
    }

    private static void finishActiveCuboids(
            Map<RectangleKey, Integer> active,
            int maxY,
            List<CuboidData> finished
    ) {

        for (Map.Entry<RectangleKey, Integer> entry :
                active.entrySet()) {

            finished.add(
                    new CuboidData(
                            entry.getKey(),
                            entry.getValue(),
                            maxY
                    )
            );
        }
    }

    /**
     * XZ 平面上的矩形。
     */
    private record RectangleKey(
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
    }

    /**
     * 一行中的连续 X 区间。
     */
    private record XInterval(
            int minX,
            int maxX
    ) {
    }

    /**
     * 一个最终的三维 Cuboid。
     */
    private record CuboidData(
            RectangleKey rectangle,
            int minY,
            int maxY
    ) {
    }
}