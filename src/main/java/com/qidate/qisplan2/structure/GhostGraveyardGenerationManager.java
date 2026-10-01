package com.qidate.qisplan2.structure;

import com.qidate.qisplan2.QisPlan2;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class GhostGraveyardGenerationManager {

    private static GhostGraveyardGenerationTask activeTask;

    private GhostGraveyardGenerationManager() {
    }

    /**
     * 开始生成鬼坟场。
     *
     * @return false = 已经有鬼坟场正在生成
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
                    new GhostGraveyardGenerationTask(
                            level,
                            origin
                    );

        } catch (Exception e) {

            QisPlan2.LOGGER.error(
                    "[QisPlan2] 无法开始生成鬼坟场",
                    e
            );

            activeTask = null;

            return false;
        }

        QisPlan2.LOGGER.info(
                "[QisPlan2] 开始生成鬼坟场，原点：{}，Part尺寸：{}×{}",
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