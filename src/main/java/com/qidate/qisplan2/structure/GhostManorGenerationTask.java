package com.qidate.qisplan2.structure;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class GhostManorGenerationTask {

    /*
     * ghost_manor 实际拆出的尺寸：
     *
     * X = 10 个 Chunk
     * Z = 16 个 Chunk
     */
    private static final int PARTS_X = 10;
    private static final int PARTS_Z = 16;

    private final ServerLevel level;

    /**
     * 整栋庄园的世界原点。
     *
     * X/Z 会自动对齐到 Chunk 边界。
     */
    private final BlockPos origin;

    /**
     * 当前正在生成的子结构。
     */
    private int currentX = 0;
    private int currentZ = 0;

    /**
     * 是否正在等待 Chunk 完成。
     */
    private boolean waitingForChunk = false;

    /**
     * 是否已经完成。
     */
    private boolean finished = false;

    public GhostManorGenerationTask(
            ServerLevel level,
            BlockPos origin
    ) {
        this.level = level;

        /*
         * X/Z 强制对齐 Chunk 边界。
         */
        this.origin = new BlockPos(
                origin.getX() >> 4 << 4,
                origin.getY(),
                origin.getZ() >> 4 << 4
        );
    }

    public boolean isFinished() {
        return finished;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    /**
     * 每个 Server Tick 调用一次。
     */
    public void tick() {

        if (finished) {
            return;
        }

        /*
         * 当前已经在等待 Chunk。
         */
        if (waitingForChunk) {
            return;
        }

        /*
         * 所有 Part 都完成。
         */
        if (currentZ >= PARTS_Z) {

            finished = true;

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 鬼庄园生成完成！"
            );

            return;
        }

        /*
         * 当前子结构的世界位置。
         */
        BlockPos partPos =
                origin.offset(
                        currentX * 16,
                        0,
                        currentZ * 16
                );

        int chunkX =
                partPos.getX() >> 4;

        int chunkZ =
                partPos.getZ() >> 4;

        QisPlan2.LOGGER.info(
                "[QisPlan2] 鬼庄园生成 Part [{}, {}]，目标 Chunk [{}, {}]",
                currentX,
                currentZ,
                chunkX,
                chunkZ
        );

        waitingForChunk = true;

        ServerChunkCache chunkSource =
                level.getChunkSource();

        /*
         * 请求目标 Chunk 达到 FULL。
         *
         * true = 如果不存在则允许生成。
         */
        chunkSource.getChunkFuture(
                chunkX,
                chunkZ,
                ChunkStatus.FULL,
                true
        ).thenAccept(result -> {

            /*
             * Future 回调不一定运行在 Server Thread，
             * 所以重新切回服务器线程。
             */
            level.getServer().execute(() -> {

                try {

                    if (!result.isSuccess()) {

                        QisPlan2.LOGGER.error(
                                "[QisPlan2] Chunk [{}, {}] 生成失败：{}",
                                chunkX,
                                chunkZ,
                                result.getError()
                        );

                        /*
                         * 目前测试阶段：
                         * 直接结束任务。
                         */
                        finished = true;

                        return;
                    }

                    /*
                     * Chunk 已经 FULL。
                     *
                     * 现在才放置 NBT。
                     */
                    placePart(
                            currentX,
                            currentZ,
                            partPos
                    );

                    /*
                     * 当前 Part 完成。
                     */
                    advance();

                } catch (Exception e) {

                    QisPlan2.LOGGER.error(
                            "[QisPlan2] 生成鬼庄园 Part [{}, {}] 失败",
                            currentX,
                            currentZ,
                            e
                    );

                    finished = true;

                } finally {

                    waitingForChunk = false;
                }
            });
        });
    }

    /**
     * 放置一个鬼庄园子结构。
     */
    private void placePart(
            int partX,
            int partZ,
            BlockPos partPos
    ) {

        ResourceLocation structureId =
                ResourceLocation.fromNamespaceAndPath(
                        QisPlan2.MODID,
                        "ghost_manor_parts/x"
                                + partX
                                + "_z"
                                + partZ
                );

        StructureTemplateManager manager =
                level.getStructureManager();

        StructureTemplate template =
                manager.get(structureId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "找不到结构："
                                                + structureId
                                )
                        );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 放置 {}，大小 {}，位置 {}",
                structureId,
                template.getSize(),
                partPos
        );

        StructurePlaceSettings settings =
                new StructurePlaceSettings();

        boolean success =
                template.placeInWorld(
                        level,
                        partPos,
                        partPos,
                        settings,
                        level.random,
                        2
                );

        if (!success) {

            throw new IllegalStateException(
                    "StructureTemplate.placeInWorld() 返回 false："
                            + structureId
            );
        }
    }

    /**
     * 前进到下一个 Part。
     */
    private void advance() {

        currentX++;

        if (currentX >= PARTS_X) {

            currentX = 0;
            currentZ++;
        }
    }
}