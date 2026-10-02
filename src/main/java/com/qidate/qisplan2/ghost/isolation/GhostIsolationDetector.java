package com.qidate.qisplan2.ghost.isolation;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * 灵异隔绝空间检测器。
 *
 * <p>
 * 从指定位置开始，对周围能够传播灵异的空间进行
 * Flood Fill（洪水填充）检测。
 *
 * <p>
 * 如果所有能够到达的空间最终都被
 * {@link GhostIsolationBlockRegistry} 注册的隔绝方块封闭，
 * 则认为当前位置处于灵异隔绝空间。
 *
 * <p>
 * 如果搜索过程中超过最大搜索范围，
 * 则无法确定整个空间是否真正封闭，返回 UNKNOWN。
 */
public final class GhostIsolationDetector {

    /**
     * 单次检测允许访问的最大方块数量。
     *
     * <p>
     * 这是为了防止在开放世界中进行无限搜索。
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
         * 那么玩家实际上不应该处于这个方块内部。
         *
         * 第一版这里直接返回 UNKNOWN，
         * 避免对异常情况做错误判断。
         */
        BlockState startState =
                level.getBlockState(start);

        if (GhostIsolationBlockRegistry.canBlockSupernatural(
                startState
        )) {
            return DetectionResult.unknown();
        }

        /*
         * BFS 队列。
         *
         * 使用 BlockPos.MutableBlockPos 时虽然可以减少对象创建，
         * 但会增加这里的实现复杂度。
         *
         * 第一版先使用普通 BlockPos，
         * 等系统稳定后再针对性能进行优化。
         */
        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        /*
         * 已经访问过的位置。
         *
         * Flood Fill 必须记录访问过的位置，
         * 否则相邻空间之间会不断重复搜索。
         */
        Set<BlockPos> visited =
                new HashSet<>();

        /*
         * 起点加入搜索队列。
         */
        BlockPos startPos =
                start.immutable();

        queue.add(startPos);
        visited.add(startPos);

        /*
         * 用于记录整个可传播空间的包围盒。
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
         * 开始 Flood Fill。
         */
        while (!queue.isEmpty()) {

            /*
             * 搜索数量达到上限。
             *
             * 这意味着我们无法证明整个空间已经被封闭。
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
                 * 已经搜索过的位置无需重复处理。
                 */
                if (visited.contains(next)) {
                    continue;
                }

                /*
                 * 不主动加载未加载区块。
                 *
                 * 因为检测器不应该为了判断灵异隔绝，
                 * 强行把大量区块加载进服务器。
                 *
                 * 如果空间已经延伸到了当前已加载区域之外，
                 * 第一版无法确定那里是否存在隔绝结构。
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
                 *
                 * 因此这个方向到此为止，
                 * 不继续进入该方块。
                 */
                if (GhostIsolationBlockRegistry.canBlockSupernatural(
                        state
                )) {
                    continue;
                }

                /*
                 * 这是一个可以继续传播灵异的空间。
                 */
                visited.add(next);
                queue.addLast(next);

                /*
                 * 更新包围盒。
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
         * 队列已经完全耗尽，
         * 说明所有能够传播到的空间都已经被隔绝方块封闭。
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
                startPos
        );
    }

    /**
     * 一次检测的结果。
     */
    public record DetectionResult(
            IsolationState state,
            BlockPos min,
            BlockPos max,
            BlockPos seed
    ) {

        /**
         * 创建一个已经确认的隔绝结果。
         */
        public static DetectionResult isolated(
                BlockPos min,
                BlockPos max,
                BlockPos seed
        ) {

            return new DetectionResult(
                    IsolationState.ISOLATED,
                    min,
                    max,
                    seed
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
                    null
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