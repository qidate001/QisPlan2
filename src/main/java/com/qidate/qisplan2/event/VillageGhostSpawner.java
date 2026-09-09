package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class VillageGhostSpawner {

    private static final float SPAWN_CHANCE = 0.20F;

    private static final int MAX_TASKS_PER_TICK = 2;

    private record PendingChunk(
            ResourceKey<Level> dimension,
            ChunkPos chunkPos
    ) {}

    private static final ConcurrentLinkedQueue<PendingChunk> QUEUE =
            new ConcurrentLinkedQueue<>();

    /**
     * 当前运行期间，已经检查过的 Chunk。
     *
     * 注意：
     * 真正的永久记录在 VillageGhostData。
     */
    private static final java.util.Set<Long> CHECKED_CHUNKS =
            java.util.HashSet.newHashSet(1024);


    // =========================================================
    // Chunk 加载
    // =========================================================

    @SubscribeEvent
    public static void onChunkLoad(
            ChunkEvent.Load event
    ) {

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        /*
         * 目前只让主世界产生村庄鬼。
         */
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        /*
         * 不在 Chunk 加载过程中做 StructureManager 查询。
         *
         * 这里只加入队列。
         */
        QUEUE.add(
                new PendingChunk(
                        level.dimension(),
                        chunk.getPos()
                )
        );
    }


    // =========================================================
    // Server Tick
    // =========================================================

    @SubscribeEvent
    public static void onLevelTick(
            LevelTickEvent.Post event
    ) {

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        for (int i = 0; i < MAX_TASKS_PER_TICK; i++) {

            PendingChunk task =
                    QUEUE.poll();

            if (task == null) {
                return;
            }

            if (task.dimension() != level.dimension()) {
                continue;
            }

            processChunk(
                    level,
                    task.chunkPos()
            );
        }
    }


    // =========================================================
    // 处理一个 Chunk
    // =========================================================

    private static void processChunk(
            ServerLevel level,
            ChunkPos chunkPos
    ) {

        long chunkKey =
                chunkPos.toLong();

        /*
         * 同一次运行期间，这个 Chunk 不再重复检查。
         */
        if (!CHECKED_CHUNKS.add(chunkKey)) {
            return;
        }


        /*
         * 取 Chunk 中心。
         *
         * 注意：
         * 这里仅仅作为寻找 StructureStart 的探测点。
         */
        BlockPos center =
                chunkPos.getMiddleBlockPosition(64);


        /*
         * 寻找包含这个位置的 Village。
         */
        StructureStart village =
                level.structureManager()
                        .getStructureWithPieceAt(
                                center,
                                StructureTags.VILLAGE
                        );


        if (!village.isValid()) {
            return;
        }


        /*
         * -----------------------------------------------------
         * 找到真正的 Village StructureStart
         * -----------------------------------------------------
         *
         * StructureStart 有自己的生成 Chunk。
         *
         * 这个坐标才是我们用来识别“这个村庄”的东西。
         */
        ChunkPos villageChunk =
                village.getChunkPos();


        String villageId =
                createVillageId(villageChunk);


        /*
         * -----------------------------------------------------
         * 永久数据检查
         * -----------------------------------------------------
         */
        VillageGhostData data =
                VillageGhostData.get(level);


        if (data.isProcessed(villageId)) {

            /*
             * 这个村庄以前已经抽过了。
             *
             * 无论以前是：
             *
             * 20% 抽中了
             * 还是
             * 80% 没抽中
             *
             * 都不能再次抽。
             */
            return;
        }


        /*
         * -----------------------------------------------------
         * 现在正式把这个村庄标记为“已经处理”
         * -----------------------------------------------------
         *
         * 注意一定要在抽奖之前标记。
         *
         * 否则如果生成过程中发生异常，
         * 下一次加载可能再次抽奖。
         */
        data.markProcessed(villageId);


        /*
         * -----------------------------------------------------
         * 20% 概率
         * -----------------------------------------------------
         */

        RandomSource random =
                level.random;

        if (random.nextFloat() >= SPAWN_CHANCE) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 村庄 {} 没有发生灵异事件。",
                    villageId
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * 抽中了
         * -----------------------------------------------------
         */

        QisPlan2.LOGGER.info(
                "[QisPlan2] 村庄 {} 触发灵异事件！",
                villageId
        );


        spawnGhost(
                level,
                village
        );
    }


    // =========================================================
    // Village ID
    // =========================================================

    private static String createVillageId(
            ChunkPos chunkPos
    ) {

        return "village:"
                + chunkPos.x
                + ":"
                + chunkPos.z;
    }


    // =========================================================
    // 生成鬼
    // =========================================================

    private static void spawnGhost(
            ServerLevel level,
            StructureStart village
    ) {

        /*
         * 使用整个村庄的 BoundingBox。
         *
         * 不再使用 Chunk 中心。
         */
        var box =
                village.getBoundingBox();


        /*
         * 村庄中心。
         */
        int centerX =
                (box.minX() + box.maxX()) / 2;

        int centerZ =
                (box.minZ() + box.maxZ()) / 2;


        /*
         * 找到村庄中心地面高度。
         */
        BlockPos ground =
                level.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(
                                centerX,
                                0,
                                centerZ
                        )
                );


        BlockPos spawnPos =
                ground.above();


        /*
         * 三种鬼随机选择。
         */
        int type =
                level.random.nextInt(3);


        Entity entity;


        switch (type) {

            case 0 -> {

                entity =
                        ModEntities.OPENING_GHOST
                                .get()
                                .create(level);
            }

            case 1 -> {

                entity =
                        ModEntities.CLOSING_GHOST
                                .get()
                                .create(level);
            }

            default -> {

                entity =
                        ModEntities.KNOCKING_GHOST
                                .get()
                                .create(level);
            }
        }


        if (entity == null) {

            QisPlan2.LOGGER.error(
                    "[QisPlan2] 创建村庄鬼实体失败！"
            );

            return;
        }


        /*
         * 放置实体。
         */
        entity.moveTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F,
                0.0F
        );


        /*
         * 加入世界。
         */
        level.addFreshEntity(entity);


        QisPlan2.LOGGER.info(
                "[QisPlan2] 在村庄 {} 生成 {}，位置 {}",
                createVillageId(village.getChunkPos()),
                entity.getType(),
                spawnPos
        );
    }
}