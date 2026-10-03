package com.qidate.qisplan2.ghost.isolation;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 灵异隔绝空间检测器。
 *
 * <p>
 * 从指定位置开始，对周围能够传播灵异的空间进行
 * Flood Fill（洪水填充）检测。
 *
 * <p>
 * 检测会按照以下顺序进行：
 *
 * <ol>
 *     <li>起点本身是灵异隔绝方块 → UNKNOWN</li>
 *     <li>当前位置能够直接向上连通天空 → OPEN</li>
 *     <li>否则进行 Flood Fill</li>
 * </ol>
 *
 * <p>
 * 如果所有能够到达的空间最终都被
 * {@link GhostIsolationBlockRegistry} 注册的隔绝方块封闭，
 * 则认为当前位置处于灵异隔绝空间。
 *
 * <p>
 * Flood Fill 完成后，
 * 会将精确搜索到的空间拆分成多个
 * {@link GhostIsolationCuboid}，
 * 用于后续精确表示整个隔绝空间。
 */
public final class GhostIsolationDetector {

    /**
     * 单次检测允许访问的最大方块数量。
     */
    private static final int MAX_SEARCH_BLOCKS = 8192;

    private GhostIsolationDetector() {
    }

    /**
     * 对指定位置进行一次灵异隔绝检测。
     *
     * @param level 世界
     * @param start 检测起点
     * @return 检测结果
     */
    public static DetectionResult detect(
            ServerLevel level,
            BlockPos start
    ) {

        /*
         * 如果起点本身就是隔绝方块，
         * 无法从该位置判断其内部空间，
         * 因此直接返回 UNKNOWN。
         */
        BlockState startState =
                level.getBlockState(start);

        if (GhostIsolationBlockRegistry.canBlockSupernatural(
                startState
        )) {
            return DetectionResult.unknown();
        }

        /*
         * ========================================================
         * 第一阶段：天空连通性检测
         * ========================================================
         *
         * 只要当前位置沿 Y 轴向上，
         * 一直到世界顶部都没有遇到能够阻断灵异传播的方块，
         * 那么当前位置必然属于开放空间。
         */
        if (isOpenToSky(level, start)) {

            return DetectionResult.open();
        }

        /*
         * ========================================================
         * 第二阶段：Flood Fill
         * ========================================================
         */

        /*
         * BFS 队列。
         */
        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        /*
         * 已访问空间。
         *
         * 注意：
         * 这里的数据现在不仅用于防止重复搜索，
         * 还会在 Flood Fill 完成后交给
         * GhostIsolationCuboidDecomposer。
         */
        Set<BlockPos> visited =
                new HashSet<>();

        BlockPos startPos =
                start.immutable();

        queue.add(startPos);
        visited.add(startPos);

        /*
         * 整个空间的粗略包围盒。
         *
         * 只用于 AABB 粗筛，
         * 不能单独表示真实空间形状。
         */
        int minX = start.getX();
        int minY = start.getY();
        int minZ = start.getZ();

        int maxX = start.getX();
        int maxY = start.getY();
        int maxZ = start.getZ();

        /*
         * 六个方向。
         */
        final int[][] DIRECTIONS = {
                {1, 0, 0},
                {-1, 0, 0},
                {0, 1, 0},
                {0, -1, 0},
                {0, 0, 1},
                {0, 0, -1}
        };

        /*
         * ========================================================
         * Flood Fill
         * ========================================================
         */
        while (!queue.isEmpty()) {

            /*
             * 搜索达到上限。
             *
             * 无法证明整个空间已经封闭。
             */
            if (visited.size() >= MAX_SEARCH_BLOCKS) {

                QisPlan2.LOGGER.info(
                        "[灵异隔绝检测] 搜索达到上限 {}，当前位置：{}",
                        MAX_SEARCH_BLOCKS,
                        start
                );

                return DetectionResult.unknown();
            }

            BlockPos current =
                    queue.removeFirst();

            /*
             * 检查六个相邻方向。
             */
            for (int[] direction :
                    DIRECTIONS) {

                BlockPos next =
                        current.offset(
                                direction[0],
                                direction[1],
                                direction[2]
                        );

                /*
                 * 已经访问过的位置无需重复处理。
                 */
                if (visited.contains(next)) {
                    continue;
                }

                /*
                 * 不主动加载未加载区块。
                 */
                if (!level.hasChunkAt(next)) {

                    QisPlan2.LOGGER.info(
                            "[灵异隔绝检测] 搜索到未加载区块，当前位置：{}，已搜索：{} 个方块",
                            next,
                            visited.size()
                    );

                    return DetectionResult.unknown();
                }

                BlockState state =
                        level.getBlockState(next);

                /*
                 * 隔绝方块阻断灵异传播。
                 */
                if (GhostIsolationBlockRegistry.canBlockSupernatural(
                        state
                )) {
                    continue;
                }

                /*
                 * 加入可传播空间。
                 */
                visited.add(next);
                queue.addLast(next);

                /*
                 * 更新粗略包围盒。
                 */
                minX =
                        Math.min(
                                minX,
                                next.getX()
                        );

                minY =
                        Math.min(
                                minY,
                                next.getY()
                        );

                minZ =
                        Math.min(
                                minZ,
                                next.getZ()
                        );

                maxX =
                        Math.max(
                                maxX,
                                next.getX()
                        );

                maxY =
                        Math.max(
                                maxY,
                                next.getY()
                        );

                maxZ =
                        Math.max(
                                maxZ,
                                next.getZ()
                        );
            }
        }

        /*
         * ========================================================
         * Flood Fill 完成。
         *
         * 此时 visited 就是整个精确空间。
         * ========================================================
         */
        List<GhostIsolationCuboid> cuboids =
                GhostIsolationCuboidDecomposer.decompose(
                        visited
                );

        /*
         * ========================================================
         * 创建最终检测结果。
         * ========================================================
         */
        return DetectionResult.isolated(
                new BlockPos(
                        minX,
                        minY,
                        minZ
                ),
                new BlockPos(
                        maxX,
                        maxY,
                        maxZ
                ),
                startPos,
                cuboids
        );
    }

    /**
     * 检查指定位置是否能够沿 Y 轴直接连通天空。
     *
     * <p>
     * 这里只判断“是否存在能够阻断灵异传播的方块”，
     * 而不是判断方块是不是空气。
     *
     * <p>
     * 因此普通石头、玻璃、树叶等方块，
     * 如果没有注册到
     * {@link GhostIsolationBlockRegistry}，
     * 就不会阻断检测。
     *
     * @param level 世界
     * @param start 起点
     * @return 如果能够连通世界顶部，则返回 {@code true}
     */
    private static boolean isOpenToSky(
            ServerLevel level,
            BlockPos start
    ) {

        int maxY =
                level.getMaxBuildHeight();

        /*
         * 从起点的上一个方块开始检查。
         *
         * 起点本身已经在 detect() 中检查过，
         * 因此这里无需再次检查。
         */
        for (int y = start.getY() + 1;
             y < maxY;
             y++) {

            BlockPos pos =
                    new BlockPos(
                            start.getX(),
                            y,
                            start.getZ()
                    );

            /*
             * 不主动加载未加载区块。
             *
             * 如果天空检测过程中发现对应 Chunk 未加载，
             * 就不能证明它是 OPEN。
             *
             * 因此返回 false，
             * 让后续 Flood Fill 决定最终结果。
             */
            if (!level.hasChunkAt(pos)) {
                return false;
            }

            BlockState state =
                    level.getBlockState(pos);

            /*
             * 只有注册的灵异隔绝方块
             * 才能够阻断向天空的灵异传播。
             */
            if (GhostIsolationBlockRegistry.canBlockSupernatural(
                    state
            )) {
                return false;
            }
        }

        /*
         * 从当前位置一直到世界顶部，
         * 都没有遇到灵异隔绝方块。
         *
         * 因此当前位置必然属于开放空间。
         */
        return true;
    }

    /**
     * 一次检测的结果。
     */
    public record DetectionResult(
            IsolationState state,
            BlockPos min,
            BlockPos max,
            BlockPos seed,
            List<GhostIsolationCuboid> cuboids
    ) {

        /**
         * 创建一个已经确认的隔绝结果。
         */
        public static DetectionResult isolated(
                BlockPos min,
                BlockPos max,
                BlockPos seed,
                List<GhostIsolationCuboid> cuboids
        ) {

            return new DetectionResult(
                    IsolationState.ISOLATED,
                    min,
                    max,
                    seed,
                    cuboids
            );
        }

        /**
         * 创建一个已经确认的开放空间结果。
         */
        public static DetectionResult open() {

            return new DetectionResult(
                    IsolationState.OPEN,
                    null,
                    null,
                    null,
                    List.of()
            );
        }

        /**
         * 创建一个无法确定的结果。
         */
        public static DetectionResult unknown() {

            return new DetectionResult(
                    IsolationState.UNKNOWN,
                    null,
                    null,
                    null,
                    List.of()
            );
        }

        /**
         * 判断检测是否确认了一个隔绝区域。
         */
        public boolean isIsolated() {

            return state ==
                    IsolationState.ISOLATED;
        }
    }
}