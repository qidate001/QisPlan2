package com.qidate.qisplan2.util;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

public class StructureDebug {

    public static void test(ServerLevel level, BlockPos pos) {

        QisPlan2.LOGGER.info("========== Jigsaw Structure Debug ==========");

        ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(
                        QisPlan2.MODID,
                        "ghost_temple_test"
                );

        StructureTemplateManager manager =
                level.getStructureManager();

        QisPlan2.LOGGER.info(
                "[DEBUG] StructureTemplateManager 中的结构列表："
        );

        var resourceManager =
                level.getServer().getResourceManager();

        resourceManager
                .listResources(
                        "structures",
                        location ->
                                location.getPath().endsWith(".nbt")
                                        && location.getNamespace()
                                        .equals(QisPlan2.MODID)
                )
                .forEach((location, resource) ->
                        QisPlan2.LOGGER.info(
                                "[DEBUG] ResourceManager 列表发现: {}",
                                location
                        )
                );

        manager.listTemplates().forEach(
                templateId -> {
                    if (templateId.getNamespace().equals(QisPlan2.MODID)) {
                        QisPlan2.LOGGER.info(
                                "[DEBUG] 找到模板: {}",
                                templateId
                        );
                    }
                }
        );

        ResourceLocation resourceId =
                ResourceLocation.fromNamespaceAndPath(
                        QisPlan2.MODID,
                        "structures/ghost_temple.nbt"
                );

        var resource =
                level.getServer()
                        .getResourceManager()
                        .getResource(resourceId);

        if (resource.isPresent()) {
            QisPlan2.LOGGER.info(
                    "[DEBUG] ResourceManager ✅ 找到: {}",
                    resourceId
            );
        } else {
            QisPlan2.LOGGER.error(
                    "[DEBUG] ResourceManager ❌ 找不到: {}",
                    resourceId
            );
        }

        // -------------------------------------------------
        // 1. StructureTemplateManager
        // -------------------------------------------------

        QisPlan2.LOGGER.info(
                "[1] StructureTemplateManager.get(): {}",
                id
        );

        Optional<StructureTemplate> managed =
                manager.get(id);

        if (managed.isEmpty()) {

            QisPlan2.LOGGER.error(
                    "[1] ❌ manager.get() = empty"
            );

        } else {

            StructureTemplate t = managed.get();

            QisPlan2.LOGGER.info(
                    "[1] ✅ manager.get() 成功"
            );

            QisPlan2.LOGGER.info(
                    "[1] manager.get() Size = {}",
                    t.getSize()
            );
        }

        // 2. 检查 Template Pool
        QisPlan2.LOGGER.info(
                "[2] 检查 Template Pool: {}",
                id
        );

        var registry =
                level.registryAccess()
                        .registryOrThrow(Registries.TEMPLATE_POOL);

        Optional<net.minecraft.core.Holder.Reference<StructureTemplatePool>> holder =
                registry.getHolder(id);

        if (holder.isEmpty()) {
            QisPlan2.LOGGER.error(
                    "[2] ❌ Template Pool 找不到: {}",
                    id
            );
            return;
        }

        StructureTemplatePool pool = holder.get().value();

        QisPlan2.LOGGER.info("[2] ✅ Template Pool 找到了");

        // 3. 从 Pool 随机获取 Element
        StructurePoolElement element =
                pool.getRandomTemplate(level.random);

        QisPlan2.LOGGER.info(
                "[3] Element 类型: {}",
                element.getClass().getName()
        );

        // 4. 检查是不是 SinglePoolElement
        if (!(element instanceof SinglePoolElement single)) {
            QisPlan2.LOGGER.error(
                    "[4] ❌ 不是 SinglePoolElement"
            );
            return;
        }

        QisPlan2.LOGGER.info(
                "[4] ✅ 是 SinglePoolElement"
        );

        // 5. 获取 BoundingBox
        BoundingBox box =
                single.getBoundingBox(
                        manager,
                        pos,
                        net.minecraft.world.level.block.Rotation.NONE
                );

        QisPlan2.LOGGER.info(
                "[5] BoundingBox: {}",
                box
        );

        QisPlan2.LOGGER.info(
                "[5] Min: {}, {}, {}",
                box.minX(),
                box.minY(),
                box.minZ()
        );

        QisPlan2.LOGGER.info(
                "[5] Max: {}, {}, {}",
                box.maxX(),
                box.maxY(),
                box.maxZ()
        );

        QisPlan2.LOGGER.info(
                "==========================================="
        );
    }
}