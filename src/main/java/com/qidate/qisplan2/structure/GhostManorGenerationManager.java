package com.qidate.qisplan2.structure;

import com.qidate.qisplan2.QisPlan2;
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

        if (activeTask != null
                && !activeTask.isFinished()) {

            return false;
        }

        activeTask =
                new GhostManorGenerationTask(
                        level,
                        origin
                );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 开始生成鬼庄园，原点：{}",
                activeTask.getOrigin()
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