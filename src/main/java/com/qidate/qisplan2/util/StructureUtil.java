package com.qidate.qisplan2.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

public class StructureUtil {

    private StructureUtil() {
    }

    /**
     * 在指定位置生成结构
     */
    public static boolean placeStructure(
            ServerLevel level,
            BlockPos pos,
            String structureId
    ) {

        ResourceLocation id = ResourceLocation.parse(structureId);

        Optional<StructureTemplate> optional =
                level.getStructureManager().get(id);

        // 找不到结构
        if (optional.isEmpty()) {
            System.err.println(
                    "[QisPlan2] 找不到结构: " + id
            );
            return false;
        }

        StructureTemplate template = optional.get();

        System.out.println(
                "[QisPlan2] 正在生成结构: " + id
        );

        System.out.println(
                "[QisPlan2] 结构大小: " + template.getSize()
        );

        StructurePlaceSettings settings =
                new StructurePlaceSettings();

        boolean result = template.placeInWorld(
                level,
                pos,
                pos,
                settings,
                level.random,
                2
        );

        System.out.println(
                "[QisPlan2] 结构生成结果: " + result
        );

        return result;
    }
}