package com.qidate.qisplan2.ghost.module.host.item;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * 灵异物品数据访问接口。
 *
 * 统一管理 ItemStack 上保存的灵异模块实例数据。
 * 每个模块实例独立保存强度、复苏值、死机值和专属数据。
 *
 * ItemStack 的 Data Components 是唯一持久化数据源。
 * GhostModuleContainer 仅在单次操作期间使用，不长期缓存。
 */
public final class GhostItemData {

    private GhostItemData() {
    }

    /**
     * 获取物品携带的全部灵异模块。
     *
     * 返回不可修改的副本，避免调用方绕过数据组件直接修改列表。
     */
    public static List<GhostModuleData.Entry> getModules(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");

        return List.copyOf(
                stack.getOrDefault(
                        ModDataComponents.GHOST_MODULES.get(),
                        List.of()
                )
        );
    }

    /**
     * 判断物品是否携带指定模块。
     */
    public static boolean hasModule(
            ItemStack stack,
            ResourceLocation moduleId
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        return getModules(stack).stream()
                .anyMatch(entry -> entry.id().equals(moduleId));
    }

    /**
     * 添加模块或更新已有模块的强度。
     *
     * 已存在的模块保留复苏值、死机值和专属数据。
     * 新模块的复苏值和死机值默认为 0。
     */
    public static void addModule(
            ItemStack stack,
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer(stack);

        container.addModule(moduleId, intensity);

        saveContainer(stack, container);
    }

    /**
     * 添加或替换完整的模块实例数据。
     *
     * 同一模块 ID 只保留一条数据。
     */
    public static void setModuleEntry(
            ItemStack stack,
            GhostModuleData.Entry newEntry
    ) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(newEntry, "newEntry");

        GhostModuleContainer container = createContainer(stack);

        container.loadEntries(
                mergeEntry(container.getModules(), newEntry)
        );

        saveContainer(stack, container);
    }

    /**
     * 批量替换物品携带的全部模块数据。
     *
     * 使用 GhostModuleContainer 统一校验和去重，
     * 最后一次性写回 ItemStack 的 Data Component。
     */
    static void setModules(
            ItemStack stack,
            List<GhostModuleData.Entry> modules
    ) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(modules, "modules");

        GhostModuleContainer container = new GhostModuleContainer();
        container.loadEntries(modules);

        List<GhostModuleData.Entry> result = container.getModules();

        if (result.isEmpty()) {
            stack.remove(ModDataComponents.GHOST_MODULES.get());
        } else {
            stack.set(
                    ModDataComponents.GHOST_MODULES.get(),
                    List.copyOf(result)
            );
        }
    }

    /**
     * 移除指定模块及其全部属性数据。
     */
    public static void removeModule(
            ItemStack stack,
            ResourceLocation moduleId
    ) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer(stack);

        if (!container.hasModule(moduleId)) {
            return;
        }

        container.removeModule(moduleId);

        saveContainer(stack, container);
    }

    /**
     * 从 ItemStack 当前的数据组件创建临时容器。
     */
    private static GhostModuleContainer createContainer(ItemStack stack) {
        GhostModuleContainer container = new GhostModuleContainer();
        container.loadEntries(getModules(stack));
        return container;
    }

    /**
     * 将容器中的模块列表写回 ItemStack。
     */
    private static void saveContainer(
            ItemStack stack,
            GhostModuleContainer container
    ) {
        List<GhostModuleData.Entry> modules = container.getModules();

        if (modules.isEmpty()) {
            stack.remove(ModDataComponents.GHOST_MODULES.get());
        } else {
            stack.set(
                    ModDataComponents.GHOST_MODULES.get(),
                    List.copyOf(modules)
            );
        }
    }

    /**
     * 生成去重后的模块列表，并将新条目放在列表末尾。
     *
     * 使用完整条目替换，确保通用属性和专属数据均以新条目为准。
     */
    private static List<GhostModuleData.Entry> mergeEntry(
            List<GhostModuleData.Entry> existing,
            GhostModuleData.Entry newEntry
    ) {
        java.util.ArrayList<GhostModuleData.Entry> result =
                new java.util.ArrayList<>(existing);

        result.removeIf(entry -> entry.id().equals(newEntry.id()));
        result.add(newEntry);

        return List.copyOf(result);
    }
}