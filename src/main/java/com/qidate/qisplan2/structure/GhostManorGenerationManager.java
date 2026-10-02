package com.qidate.qisplan2.structure;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationBatchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class GhostManorGenerationManager {

    private static GhostManorGenerationTask activeTask;

    private GhostManorGenerationManager() {
    }

    /**
     * 开始生成鬼庄园。
     *
     * @return false = 已经有任务正在生成
     */
    public static boolean start(
            ServerLevel level,
            BlockPos origin
    ) {

        /*
         * 如果旧任务已经结束，
         * 确保它的 Batch 已经提交。
         */
        if (activeTask != null
                && activeTask.isFinished()) {

            GhostIsolationBatchManager.end(
                    level
            );

            activeTask = null;
        }

        if (activeTask != null) {
            return false;
        }

        GhostManorGenerationTask task =
                new GhostManorGenerationTask(
                        level,
                        origin
                );

        /*
         * 先记录任务。
         */
        activeTask = task;

        /*
         * 鬼庄园整个生成范围：
         *
         * X = 10 Chunk
         * Z = 16 Chunk
         *
         * 在生成开始前，
         * 一次性告诉灵异隔绝系统：
         *
         * “这一大片区域接下来会发生大量方块变化。”
         */
        GhostIsolationBatchManager.begin(
                level,
                task.getMinChunkX(),
                task.getMaxChunkX(),
                task.getMinChunkZ(),
                task.getMaxChunkZ()
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 开始生成鬼庄园，原点：{}，Chunk 范围：X {}~{}，Z {}~{}",
                task.getOrigin(),
                task.getMinChunkX(),
                task.getMaxChunkX(),
                task.getMinChunkZ(),
                task.getMaxChunkZ()
        );

        return true;
    }

    /**
     * 每个服务器 Tick 调用。
     */
    public static void tick() {

        if (activeTask == null) {
            return;
        }

        if (activeTask.isFinished()) {

            /*
             * 鬼庄园已经生成完成。
             *
             * 现在才一次性提交整个 Batch。
             */
            GhostIsolationBatchManager.end(
                    activeTask.getLevel()
            );

            activeTask = null;

            return;
        }

        activeTask.tick();
    }

    public static boolean isRunning() {
        return activeTask != null
                && !activeTask.isFinished();
    }
}