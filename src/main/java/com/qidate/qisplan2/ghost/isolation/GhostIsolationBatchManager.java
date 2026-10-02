package com.qidate.qisplan2.ghost.isolation;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.server.level.ServerLevel;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 灵异隔绝系统的大规模方块变更批处理管理器。
 *
 * <p>
 * 当大型结构、WorldGen 等一次性修改大量方块时，
 * 可以提前声明一个 Chunk 范围。
 *
 * <p>
 * 在 Batch 持续期间，该范围内的方块变化不会立即触发
 * 灵异隔绝 Region 失效。
 *
 * <p>
 * Batch 结束后，再统一让受影响 Region 失效。
 */
public final class GhostIsolationBatchManager {

    private static final Map<
            ServerLevel,
            Batch
            > BATCHES =
            new IdentityHashMap<>();

    private GhostIsolationBatchManager() {
    }

    /**
     * 开始一个 Chunk 范围批处理。
     *
     * <p>
     * 如果该维度已经存在 Batch，
     * 则采用嵌套 Batch 方式处理。
     *
     * <p>
     * 内层 Batch 结束时不会立即提交，
     * 直到最外层 Batch 结束。
     */
    public static void begin(
            ServerLevel level,
            int minChunkX,
            int maxChunkX,
            int minChunkZ,
            int maxChunkZ
    ) {

        int realMinChunkX =
                Math.min(minChunkX, maxChunkX);

        int realMaxChunkX =
                Math.max(minChunkX, maxChunkX);

        int realMinChunkZ =
                Math.min(minChunkZ, maxChunkZ);

        int realMaxChunkZ =
                Math.max(minChunkZ, maxChunkZ);

        Batch batch =
                BATCHES.get(level);

        if (batch == null) {

            batch =
                    new Batch(
                            realMinChunkX,
                            realMaxChunkX,
                            realMinChunkZ,
                            realMaxChunkZ
                    );

            BATCHES.put(
                    level,
                    batch
            );

            QisPlan2.LOGGER.info(
                    "[灵异隔绝批处理] 开始 Batch，Chunk 范围：X {}~{}，Z {}~{}",
                    realMinChunkX,
                    realMaxChunkX,
                    realMinChunkZ,
                    realMaxChunkZ
            );

            return;
        }

        /*
         * 已经存在 Batch。
         *
         * 扩大范围并增加嵌套深度。
         */
        batch.depth++;

        batch.minChunkX =
                Math.min(
                        batch.minChunkX,
                        realMinChunkX
                );

        batch.maxChunkX =
                Math.max(
                        batch.maxChunkX,
                        realMaxChunkX
                );

        batch.minChunkZ =
                Math.min(
                        batch.minChunkZ,
                        realMinChunkZ
                );

        batch.maxChunkZ =
                Math.max(
                        batch.maxChunkZ,
                        realMaxChunkZ
                );
    }

    /**
     * 结束当前 Batch。
     *
     * <p>
     * 只有最外层 Batch 结束时，
     * 才真正提交此次批量变化。
     */
    public static void end(
            ServerLevel level
    ) {

        Batch batch =
                BATCHES.get(level);

        if (batch == null) {
            return;
        }

        /*
         * 仍然存在嵌套 Batch。
         */
        if (batch.depth > 0) {

            batch.depth--;

            return;
        }

        /*
         * 最外层 Batch 结束。
         */
        BATCHES.remove(level);

        QisPlan2.LOGGER.info(
                "[灵异隔绝批处理] Batch 完成，Chunk 范围：X {}~{}，Z {}~{}",
                batch.minChunkX,
                batch.maxChunkX,
                batch.minChunkZ,
                batch.maxChunkZ
        );

        GhostIsolationSystem.onBatchChanged(
                level,
                batch.minChunkX,
                batch.maxChunkX,
                batch.minChunkZ,
                batch.maxChunkZ
        );
    }

    /**
     * 判断指定 BlockPos 当前是否处于 Batch 范围。
     */
    public static boolean isBlocked(
            ServerLevel level,
            net.minecraft.core.BlockPos pos
    ) {

        Batch batch =
                BATCHES.get(level);

        if (batch == null) {
            return false;
        }

        int chunkX =
                pos.getX() >> 4;

        int chunkZ =
                pos.getZ() >> 4;

        return batch.contains(
                chunkX,
                chunkZ
        );
    }

    /**
     * 判断整个 Chunk 是否处于 Batch 范围。
     */
    public static boolean isBlocked(
            ServerLevel level,
            int chunkX,
            int chunkZ
    ) {

        Batch batch =
                BATCHES.get(level);

        if (batch == null) {
            return false;
        }

        return batch.contains(
                chunkX,
                chunkZ
        );
    }

    /**
     * 当前维度是否存在 Batch。
     */
    public static boolean isActive(
            ServerLevel level
    ) {

        return BATCHES.containsKey(level);
    }

    /**
     * Batch 内部状态。
     */
    private static final class Batch {

        private int minChunkX;
        private int maxChunkX;

        private int minChunkZ;
        private int maxChunkZ;

        /**
         * 嵌套深度。
         *
         * <p>
         * 0 = 最外层。
         */
        private int depth;

        private Batch(
                int minChunkX,
                int maxChunkX,
                int minChunkZ,
                int maxChunkZ
        ) {

            this.minChunkX = minChunkX;
            this.maxChunkX = maxChunkX;

            this.minChunkZ = minChunkZ;
            this.maxChunkZ = maxChunkZ;

            this.depth = 0;
        }

        private boolean contains(
                int chunkX,
                int chunkZ
        ) {

            return chunkX >= minChunkX
                    && chunkX <= maxChunkX
                    && chunkZ >= minChunkZ
                    && chunkZ <= maxChunkZ;
        }
    }
}