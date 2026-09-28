package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.curse.CurseRegistry;
import com.qidate.qisplan2.ghost.curse.CurseSavedData;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * 鬼服务器生命周期事件。
 *
 * 负责处理服务器启动、停止等生命周期事件。
 *
 * 与 GhostServerTickHandler 的区别：
 *
 * GhostServerTickHandler：
 *     处理服务器运行期间的 Tick。
 *
 * GhostServerLifecycleHandler：
 *     处理服务器启动、停止等生命周期节点。
 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class GhostServerLifecycleHandler {

    private GhostServerLifecycleHandler() {
    }

    /**
     * 服务器已经启动。
     *
     * 此时世界已经完成加载，
     * 可以安全读取 SavedData。
     */
    @SubscribeEvent
    public static void onServerStarted(
            ServerStartedEvent event
    ) {

        MinecraftServer server =
                event.getServer();

        /*
         * 确保诅咒类型已经注册。
         *
         * SavedData 恢复时需要通过 Type 找到具体 CurseType。
         */
        CurseRegistry.bootstrap();

        /*
         * 加载诅咒 SavedData。
         *
         * 如果存档中存在：
         *
         *   qisplan2_curses.dat
         *
         * 那么这里会自动读取。
         *
         * 如果不存在，
         * 则创建新的 SavedData。
         */
        CurseSavedData savedData =
                CurseSavedData.get(
                        server.overworld()
                );

        /*
         * 将存档中的诅咒恢复到运行时管理器。
         */
        savedData.restoreToManager();

        QisPlan2.LOGGER.info(
                "[服务器生命周期] 诅咒系统恢复完成，当前诅咒数量：{}",
                com.qidate.qisplan2.ghost.curse.CurseManager
                        .getAll()
                        .size()
        );
    }

    /**
     * 服务器正在停止。
     */
    @SubscribeEvent
    public static void onServerStopping(
            ServerStoppingEvent event
    ) {

        QisPlan2.LOGGER.info(
                "[服务器生命周期] 服务器正在停止"
        );

        /*
         * 清除 SavedData 的静态引用，
         * 避免下一次服务器启动时继续持有旧实例。
         */
        CurseSavedData.clearInstance();
    }
}