package com.qidate.qisplan2.structure;

import com.qidate.qisplan2.QisPlan2;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class GhostLakeGenerationManager {

    private static GhostLakeGenerationTask activeTask;

    private GhostLakeGenerationManager() {
    }

    /**
     * 开始生成鬼湖。
     *
     * @return false = 已经有鬼湖正在生成
     */
    public static boolean start(
            ServerLevel level,
            BlockPos origin
    ) {

        if (activeTask != null
                && !activeTask.isFinished()) {

            return false;
        }

        try {

            activeTask =
                    new GhostLakeGenerationTask(
                            level,
                            origin
                    );

        } catch (Exception e) {

            QisPlan2.LOGGER.error(
                    "[QisPlan2] 无法开始生成鬼湖",
                    e
            );

            activeTask = null;

            return false;
        }

        QisPlan2.LOGGER.info(
                "[QisPlan2] 开始生成鬼湖，原点：{}，Part尺寸：{}×{}",
                activeTask.getOrigin(),
                activeTask.getPartsX(),
                activeTask.getPartsZ()
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