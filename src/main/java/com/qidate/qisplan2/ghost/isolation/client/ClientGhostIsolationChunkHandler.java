package com.qidate.qisplan2.ghost.isolation.client;

import com.qidate.qisplan2.ghost.isolation.client.renderer.GhostIsolationGpuData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * 客户端灵异隔绝空间 Chunk 生命周期处理器。
 *
 * <p>
 * 负责监听客户端 Chunk 的加载与卸载，
 * 并通知灵异隔绝 GPU 数据重新构建。
 *
 * <p>
 * 这样当 Chunk 离开客户端加载范围后，
 * 对应 Cuboid 就能够及时从 GPU 数据中移除。
 */
public final class ClientGhostIsolationChunkHandler {

    private ClientGhostIsolationChunkHandler() {
    }

    /**
     * 客户端 Chunk 加载。
     *
     * <p>
     * Chunk 加载后，原本因为未加载而被过滤掉的 Cuboid
     * 可能需要重新进入 GPU 数据。
     */
    @SubscribeEvent
    public static void onChunkLoad(
            ChunkEvent.Load event
    ) {

//        if (!(event.getLevel() instanceof ClientLevel)) {
//            return;
//        }
//
//        GhostIsolationGpuData.rebuild();
    }

    /**
     * 客户端 Chunk 卸载。
     *
     * <p>
     * Chunk 卸载后，原本提交到 GPU 的 Cuboid
     * 可能已经不再满足“至少存在一个已加载 Chunk”的条件。
     */
    @SubscribeEvent
    public static void onChunkUnload(
            ChunkEvent.Unload event
    ) {

//        if (!(event.getLevel() instanceof ClientLevel)) {
//            return;
//        }
//
//        GhostIsolationGpuData.rebuild();
    }
}