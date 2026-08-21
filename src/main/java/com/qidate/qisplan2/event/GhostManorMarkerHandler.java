package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.structure.GhostManorGenerationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.ArrayDeque;
import java.util.Queue;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class GhostManorMarkerHandler {

    private static final Queue<LevelChunk> PENDING_CHUNKS =
            new ArrayDeque<>();

    @SubscribeEvent
    public static void onChunkLoad(
            ChunkEvent.Load event
    ) {

        if (!(event.getLevel()
                instanceof ServerLevel)) {
            return;
        }

        if (!(event.getChunk()
                instanceof LevelChunk chunk)) {
            return;
        }

        /*
         * 不在 ChunkEvent.Load 里直接生成。
         *
         * 这里只记录这个 Chunk。
         */
        synchronized (PENDING_CHUNKS) {
            PENDING_CHUNKS.offer(chunk);
        }
    }


    @SubscribeEvent
    public static void onServerTick(
            net.neoforged.neoforge.event.tick.ServerTickEvent.Post event
    ) {

        LevelChunk chunk;

        synchronized (PENDING_CHUNKS) {
            chunk = PENDING_CHUNKS.poll();
        }

        if (chunk == null) {
            return;
        }

        if (!(chunk.getLevel()
                instanceof ServerLevel level)) {
            return;
        }

        /*
         * 现在已经脱离 ChunkEvent.Load。
         *
         * 可以安全进行检查。
         */
        findMarker(
                level,
                chunk
        );
    }


    private static void findMarker(
            ServerLevel level,
            LevelChunk chunk
    ) {

        int minX =
                chunk.getPos().getMinBlockX();

        int minZ =
                chunk.getPos().getMinBlockZ();

        /*
         * Marker 是投影到地形表面的，
         * 所以不用扫整个 90 高度。
         *
         * 先从高度图找地表附近。
         */
        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {

                int worldX =
                        minX + x;

                int worldZ =
                        minZ + z;

                int y =
                        level.getHeight(
                                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                                worldX,
                                worldZ
                        );

                /*
                 * 在地表上下检查几格。
                 *
                 * 防止结构投影产生 1 格误差。
                 */
                for (int dy = -2; dy <= 2; dy++) {

                    BlockPos pos =
                            new BlockPos(
                                    worldX,
                                    y + dy,
                                    worldZ
                            );

                    if (!level.getBlockState(pos)
                            .is(QisPlan2.GHOST_MANOR_MARKER.get())) {
                        continue;
                    }

                    /*
                     * 找到了 Marker！
                     */
                    handleMarker(
                            level,
                            pos
                    );

                    return;
                }
            }
        }
    }


    private static void handleMarker(
            ServerLevel level,
            BlockPos pos
    ) {

        QisPlan2.LOGGER.info(
                "[QisPlan2] 发现鬼庄园 Marker：{}",
                pos
        );

        /*
         * 启动大型结构生成。
         */
        GhostManorGenerationManager.start(
                level,
                pos
        );

        /*
         * 删除 Marker。
         */
        level.removeBlock(
                pos,
                false
        );
    }
}