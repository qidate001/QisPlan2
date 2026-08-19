package com.qidate.qisplan2.util;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.IOException;

public final class StructureUtil {

    private StructureUtil() {
    }

    /**
     * 从 Mod 的 structures 目录加载并生成结构。
     *
     * 例如：
     * qisplan2:ghost_temple
     *
     * 对应：
     * data/qisplan2/structures/ghost_temple.nbt
     */
    public static boolean placeStructure(
            ServerLevel level,
            BlockPos pos,
            ResourceLocation structureId
    ) {

        ResourceLocation resourceId = ResourceLocation.fromNamespaceAndPath(
                structureId.getNamespace(),
                "structures/" + structureId.getPath() + ".nbt"
        );

        try {

            // 1. 从 ResourceManager 获取 NBT 文件
            var resourceOptional =
                    level.getServer()
                            .getResourceManager()
                            .getResource(resourceId);

            if (resourceOptional.isEmpty()) {

                QisPlan2.LOGGER.error(
                        "找不到结构文件: {}",
                        resourceId
                );

                return false;
            }

            var resource = resourceOptional.get();

            // 2. 读取 NBT
            CompoundTag tag =
                    NbtIo.readCompressed(
                            resource.open(),
                            NbtAccounter.unlimitedHeap()
                    );

            // 3. 转换为 StructureTemplate
            StructureTemplateManager manager =
                    level.getStructureManager();

            StructureTemplate template =
                    manager.readStructure(tag);

            QisPlan2.LOGGER.info(
                    "加载结构成功: {}，大小: {}",
                    structureId,
                    template.getSize()
            );

            // 4. 生成结构
            StructurePlaceSettings settings =
                    new StructurePlaceSettings();

            return template.placeInWorld(
                    level,
                    pos,
                    pos,
                    settings,
                    level.random,
                    2
            );

        } catch (IOException e) {

            QisPlan2.LOGGER.error(
                    "读取结构 NBT 失败: {}",
                    structureId,
                    e
            );

            return false;

        } catch (Exception e) {

            QisPlan2.LOGGER.error(
                    "生成结构失败: {}",
                    structureId,
                    e
            );

            return false;
        }
    }

    /**
     * 使用字符串形式的 ResourceLocation 生成结构。
     *
     * 例如：
     * StructureUtil.placeStructure(
     *     level,
     *     pos,
     *     "qisplan2:ghost_temple"
     * );
     */
    public static boolean placeStructure(
            ServerLevel level,
            BlockPos pos,
            String structureId
    ) {
        return placeStructure(
                level,
                pos,
                ResourceLocation.parse(structureId)
        );
    }
}