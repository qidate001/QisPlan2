package com.qidate.qisplan2.structure;

import com.qidate.qisplan2.QisPlan2;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class GhostLakeGenerationTask {

    private static final int CHUNK_SIZE =
            16;

    private static final ResourceLocation
            SOURCE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_lake"
            );

    private static final String PARTS_ROOT =
            "ghost_lake_parts";


    private final ServerLevel level;

    /**
     * 整个鬼湖的世界原点。
     *
     * X/Z 对齐 Chunk。
     */
    private final BlockPos origin;

    /**
     * 巨型结构尺寸。
     */
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    /**
     * Part 数量。
     */
    private final int partsX;
    private final int partsZ;

    /**
     * 当前 Part。
     */
    private int currentX = 0;
    private int currentZ = 0;

    /**
     * 是否正在等待 Chunk。
     */
    private boolean waitingForChunk = false;

    /**
     * 是否完成。
     */
    private boolean finished = false;


    public GhostLakeGenerationTask(
            ServerLevel level,
            BlockPos origin
    ) {

        this.level =
                level;

        /*
         * ========================================================
         * 读取原始巨型结构。
         * ========================================================
         */

        StructureTemplateManager manager =
                level.getStructureManager();

        StructureTemplate source =
                manager.get(
                        SOURCE_ID
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "找不到鬼湖结构："
                                        + SOURCE_ID
                        )
                );

        CompoundTag sourceTag =
                source.save(
                        new CompoundTag()
                );

        ListTag sizeTag =
                sourceTag.getList(
                        StructureTemplate.SIZE_TAG,
                        Tag.TAG_INT
                );

        if (sizeTag.size() < 3) {

            throw new IllegalStateException(
                    "鬼湖结构缺少有效尺寸信息。"
            );
        }

        this.sizeX =
                sizeTag.getInt(0);

        this.sizeY =
                sizeTag.getInt(1);

        this.sizeZ =
                sizeTag.getInt(2);

        if (sizeX <= 0
                || sizeY <= 0
                || sizeZ <= 0) {

            throw new IllegalStateException(
                    "鬼湖结构尺寸无效："
                            + sizeX
                            + "×"
                            + sizeY
                            + "×"
                            + sizeZ
            );
        }

        /*
         * ========================================================
         * 计算 Part 数量。
         * ========================================================
         */

        this.partsX =
                (sizeX + CHUNK_SIZE - 1)
                        / CHUNK_SIZE;

        this.partsZ =
                (sizeZ + CHUNK_SIZE - 1)
                        / CHUNK_SIZE;

        /*
         * ========================================================
         * X/Z 对齐 Chunk 边界。
         *
         * Y 保留玩家执行命令时的高度。
         * ========================================================
         */

        this.origin =
                new BlockPos(
                        origin.getX()
                                >> 4
                                << 4,

                        origin.getY(),

                        origin.getZ()
                                >> 4
                                << 4
                );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 鬼湖原始结构尺寸：{}×{}×{}",
                sizeX,
                sizeY,
                sizeZ
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 鬼湖拆分尺寸：{}×{} Part",
                partsX,
                partsZ
        );
    }


    public boolean isFinished() {

        return finished;
    }

    public BlockPos getOrigin() {

        return origin;
    }

    public int getPartsX() {

        return partsX;
    }

    public int getPartsZ() {

        return partsZ;
    }


    /**
     * 每个 Server Tick 调用一次。
     */
    public void tick() {

        if (finished) {
            return;
        }

        /*
         * ========================================================
         * 正在等待 Chunk。
         * ========================================================
         */

        if (waitingForChunk) {
            return;
        }

        /*
         * ========================================================
         * 所有 Part 完成。
         * ========================================================
         */

        if (currentZ >= partsZ) {

            finished = true;

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 鬼湖生成完成！"
            );

            return;
        }

        /*
         * ========================================================
         * 当前 Part 世界位置。
         * ========================================================
         */

        BlockPos partPos =
                origin.offset(
                        currentX * CHUNK_SIZE,
                        0,
                        currentZ * CHUNK_SIZE
                );

        int partX =
                currentX;

        int partZ =
                currentZ;

        int chunkX =
                partPos.getX()
                        >> 4;

        int chunkZ =
                partPos.getZ()
                        >> 4;

        QisPlan2.LOGGER.info(
                "[QisPlan2] 鬼湖生成 Part [{}, {}]，目标 Chunk [{}, {}]",
                partX,
                partZ,
                chunkX,
                chunkZ
        );

        waitingForChunk = true;

        ServerChunkCache chunkSource =
                level.getChunkSource();

        /*
         * ========================================================
         * 请求 Chunk 达到 FULL。
         *
         * 使用 whenComplete：
         *
         * 无论 Future：
         *
         * 成功
         * 失败
         * 异常
         *
         * 都一定能够解除 waitingForChunk。
         * ========================================================
         */

        chunkSource.getChunkFuture(
                chunkX,
                chunkZ,
                ChunkStatus.FULL,
                true
        ).whenComplete(
                (result, throwable) -> {

                    /*
                     * ====================================================
                     * Future 本身异常。
                     * ====================================================
                     */

                    if (throwable != null) {

                        level.getServer().execute(() -> {

                            QisPlan2.LOGGER.error(
                                    "[QisPlan2] 鬼湖 Chunk [{}, {}] Future 异常",
                                    chunkX,
                                    chunkZ,
                                    throwable
                            );

                            waitingForChunk = false;
                            finished = true;
                        });

                        return;
                    }

                    /*
                     * ====================================================
                     * 回到 Server Thread。
                     * ====================================================
                     */

                    level.getServer().execute(() -> {

                        try {

                            /*
                             * ==================================================
                             * Future 成功完成，但 ChunkResult 失败。
                             * ==================================================
                             */

                            if (result == null) {

                                QisPlan2.LOGGER.error(
                                        "[QisPlan2] 鬼湖 Chunk [{}, {}] 返回 null",
                                        chunkX,
                                        chunkZ
                                );

                                finished = true;

                                return;
                            }

                            if (!result.isSuccess()) {

                                QisPlan2.LOGGER.error(
                                        "[QisPlan2] 鬼湖 Chunk [{}, {}] 生成失败：{}",
                                        chunkX,
                                        chunkZ,
                                        result.getError()
                                );

                                finished = true;

                                return;
                            }

                            /*
                             * ==================================================
                             * Chunk 已经 FULL。
                             * ==================================================
                             */

                            QisPlan2.LOGGER.info(
                                    "[QisPlan2] 鬼湖 Chunk [{}, {}] 已达到 FULL",
                                    chunkX,
                                    chunkZ
                            );

                            /*
                             * 放置 Part。
                             */
                            placePart(
                                    partX,
                                    partZ,
                                    partPos
                            );

                            /*
                             * 当前 Part 完成。
                             */
                            advance();

                        } catch (Exception e) {

                            QisPlan2.LOGGER.error(
                                    "[QisPlan2] 生成鬼湖 Part [{}, {}] 失败",
                                    currentX,
                                    currentZ,
                                    e
                            );

                            finished = true;

                        } finally {

                            /*
                             * 无论任何情况，都解除等待。
                             */
                            waitingForChunk = false;
                        }
                    });
                }
        );
    }


    /**
     * 放置一个鬼湖子结构。
     */
    private void placePart(
            int partX,
            int partZ,
            BlockPos partPos
    ) {

        ResourceLocation structureId =
                ResourceLocation.fromNamespaceAndPath(
                        QisPlan2.MODID,
                        PARTS_ROOT
                                + "/x"
                                + partX
                                + "_z"
                                + partZ
                );

        StructureTemplateManager manager =
                level.getStructureManager();

        StructureTemplate template =
                manager.get(
                        structureId
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "找不到鬼湖 Part："
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

        if (currentX >= partsX) {

            currentX = 0;
            currentZ++;
        }
    }
}