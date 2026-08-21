package com.qidate.qisplan2.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class StructureSplitter {

    private static final int CHUNK_SIZE = 16;

    private StructureSplitter() {
    }

    /**
     * 将一个大型 StructureTemplate
     * 按 16×16 的水平区块区域拆分。
     *
     * @return 生成的小结构数量
     */
    public static int split(
            MinecraftServer server,
            ResourceLocation sourceId,
            ResourceLocation outputRoot
    ) throws IOException {

        StructureTemplateManager manager =
                server.getStructureManager();

        StructureTemplate source =
                manager.get(sourceId)
                        .orElseThrow(() ->
                                new IOException(
                                        "找不到结构模板："
                                                + sourceId
                                )
                        );

        /*
         * StructureTemplate → 原始 NBT。
         *
         * 这样可以直接处理原版结构格式，
         * 不需要访问 StructureTemplate 的 private 字段。
         */
        CompoundTag sourceTag =
                source.save(
                        new CompoundTag()
                );

        ListTag size =
                sourceTag.getList(
                        StructureTemplate.SIZE_TAG,
                        Tag.TAG_INT
                );

        int sizeX = size.getInt(0);
        int sizeY = size.getInt(1);
        int sizeZ = size.getInt(2);

        if (sizeX <= 0
                || sizeY <= 0
                || sizeZ <= 0) {

            throw new IOException(
                    "结构尺寸无效："
                            + sizeX
                            + "×"
                            + sizeY
                            + "×"
                            + sizeZ
            );
        }

        /*
         * 计算需要多少个区块。
         */
        int partsX =
                (sizeX + CHUNK_SIZE - 1)
                        / CHUNK_SIZE;

        int partsZ =
                (sizeZ + CHUNK_SIZE - 1)
                        / CHUNK_SIZE;

        int generated = 0;

        /*
         * 每一个 16×16 区域生成一个 NBT。
         */
        for (int partZ = 0;
             partZ < partsZ;
             partZ++) {

            for (int partX = 0;
                 partX < partsX;
                 partX++) {

                int startX =
                        partX * CHUNK_SIZE;

                int startZ =
                        partZ * CHUNK_SIZE;

                int partSizeX =
                        Math.min(
                                CHUNK_SIZE,
                                sizeX - startX
                        );

                int partSizeZ =
                        Math.min(
                                CHUNK_SIZE,
                                sizeZ - startZ
                        );

                CompoundTag partTag =
                        createPart(
                                sourceTag,
                                startX,
                                startZ,
                                partSizeX,
                                sizeY,
                                partSizeZ
                        );

                ResourceLocation partId =
                        ResourceLocation.fromNamespaceAndPath(
                                outputRoot.getNamespace(),
                                outputRoot.getPath()
                                        + "/x"
                                        + partX
                                        + "_z"
                                        + partZ
                        );

                Path output =
                        manager.createAndValidatePathToGeneratedStructure(
                                partId,
                                ".nbt"
                        );

                Files.createDirectories(
                        output.getParent()
                );

                NbtIo.writeCompressed(
                        partTag,
                        output
                );

                generated++;
            }
        }

        return generated;
    }


    /**
     * 从完整结构中切出一个子结构。
     */
    private static CompoundTag createPart(
            CompoundTag source,
            int startX,
            int startZ,
            int sizeX,
            int sizeY,
            int sizeZ
    ) {

        /*
         * 复制整个根 NBT。
         *
         * palette、palettes、DataVersion 等信息
         * 都保留下来。
         */
        CompoundTag part =
                source.copy();

        /*
         * ========================================
         * 修改尺寸
         * ========================================
         */
        part.put(
                StructureTemplate.SIZE_TAG,
                createIntList(
                        sizeX,
                        sizeY,
                        sizeZ
                )
        );

        /*
         * ========================================
         * Blocks
         * ========================================
         */
        ListTag sourceBlocks =
                source.getList(
                        StructureTemplate.BLOCKS_TAG,
                        Tag.TAG_COMPOUND
                );

        ListTag partBlocks =
                new ListTag();

        for (int i = 0;
             i < sourceBlocks.size();
             i++) {

            CompoundTag block =
                    sourceBlocks
                            .getCompound(i);

            ListTag pos =
                    block.getList(
                            StructureTemplate.BLOCK_TAG_POS,
                            Tag.TAG_INT
                    );

            int x = pos.getInt(0);
            int y = pos.getInt(1);
            int z = pos.getInt(2);

            /*
             * 判断这个方块是否属于当前子结构。
             */
            if (x < startX
                    || x >= startX + sizeX
                    || z < startZ
                    || z >= startZ + sizeZ) {

                continue;
            }

            /*
             * 复制方块 NBT，
             * 防止修改原始结构。
             */
            CompoundTag copied =
                    block.copy();

            /*
             * 坐标改成子结构内部坐标。
             */
            copied.put(
                    StructureTemplate.BLOCK_TAG_POS,
                    createIntList(
                            x - startX,
                            y,
                            z - startZ
                    )
            );

            partBlocks.add(copied);
        }

        part.put(
                StructureTemplate.BLOCKS_TAG,
                partBlocks
        );


        /*
         * ========================================
         * Entities
         * ========================================
         */
        if (source.contains(
                StructureTemplate.ENTITIES_TAG,
                Tag.TAG_LIST
        )) {

            ListTag sourceEntities =
                    source.getList(
                            StructureTemplate.ENTITIES_TAG,
                            Tag.TAG_COMPOUND
                    );

            ListTag partEntities =
                    new ListTag();

            for (int i = 0;
                 i < sourceEntities.size();
                 i++) {

                CompoundTag entity =
                        sourceEntities
                                .getCompound(i);

                ListTag pos =
                        entity.getList(
                                StructureTemplate.ENTITY_TAG_POS,
                                Tag.TAG_DOUBLE
                        );

                if (pos.size() < 3) {
                    continue;
                }

                double x =
                        pos.getDouble(0);

                double y =
                        pos.getDouble(1);

                double z =
                        pos.getDouble(2);

                /*
                 * 实体属于哪个子结构。
                 *
                 * 边界使用：
                 *
                 * startX <= x < endX
                 */
                if (x < startX
                        || x >= startX + sizeX
                        || z < startZ
                        || z >= startZ + sizeZ) {

                    continue;
                }

                CompoundTag copied =
                        entity.copy();

                /*
                 * 修改实体 Pos。
                 */
                ListTag newPos =
                        new ListTag();

                newPos.add(
                        net.minecraft.nbt.DoubleTag.valueOf(
                                x - startX
                        )
                );

                newPos.add(
                        net.minecraft.nbt.DoubleTag.valueOf(
                                y
                        )
                );

                newPos.add(
                        net.minecraft.nbt.DoubleTag.valueOf(
                                z - startZ
                        )
                );

                copied.put(
                        StructureTemplate.ENTITY_TAG_POS,
                        newPos
                );

                /*
                 * 修改 blockPos。
                 */
                final String ENTITY_BLOCK_POS = "blockPos";

                if (copied.contains(
                        ENTITY_BLOCK_POS,
                        Tag.TAG_LIST
                )) {

                    ListTag blockPos =
                            copied.getList(
                                    ENTITY_BLOCK_POS,
                                    Tag.TAG_INT
                            );

                    if (blockPos.size() >= 3) {

                        copied.put(
                                ENTITY_BLOCK_POS,
                                createIntList(
                                        blockPos.getInt(0) - startX,
                                        blockPos.getInt(1),
                                        blockPos.getInt(2) - startZ
                                )
                        );
                    }
                }

                partEntities.add(copied);
            }

            part.put(
                    StructureTemplate.ENTITIES_TAG,
                    partEntities
            );
        }

        return part;
    }


    /**
     * 创建整数 ListTag。
     */
    private static ListTag createIntList(
            int x,
            int y,
            int z
    ) {
        ListTag list =
                new ListTag();

        list.add(
                IntTag.valueOf(x)
        );

        list.add(
                IntTag.valueOf(y)
        );

        list.add(
                IntTag.valueOf(z)
        );

        return list;
    }
}