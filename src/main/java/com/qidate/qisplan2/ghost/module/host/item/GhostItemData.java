package com.qidate.qisplan2.ghost.module.host.item;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 灵异物品数据访问接口。
 *
 * 统一管理 ItemStack 上保存的灵异模块实例数据。
 * 每个模块实例独立保存强度、复苏值和死机值。
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
     * 已存在的模块会保留复苏值和死机值；
     * 新模块的复苏值和死机值默认均为 0。
     */
    public static void addModule(
            ItemStack stack,
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleData.Entry current = getModuleEntry(stack, moduleId);

        setModuleEntry(
                stack,
                new GhostModuleData.Entry(
                        moduleId,
                        intensity,
                        current == null ? 0.0D : current.revival(),
                        current == null ? 0.0D : current.shutdown()
                )
        );
    }

    /**
     * 添加或替换完整的模块实例数据。
     *
     * 该方法是模块数据的统一写入入口：
     * 强度、复苏值、死机值均以传入条目为准。
     */
    public static void setModuleEntry(
            ItemStack stack,
            GhostModuleData.Entry newEntry
    ) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(newEntry, "newEntry");

        List<GhostModuleData.Entry> modules =
                new ArrayList<>(getModules(stack));

        // 同一模块 ID 只保留一条数据，避免出现重复模块。
        modules.removeIf(entry ->
                entry.id().equals(newEntry.id())
        );

        modules.add(newEntry);

        stack.set(
                ModDataComponents.GHOST_MODULES.get(),
                List.copyOf(modules)
        );
    }

    /**
     * 获取指定模块的完整实例数据。
     *
     * 如果模块不存在，则返回 null。
     */
    private static GhostModuleData.Entry getModuleEntry(
            ItemStack stack,
            ResourceLocation moduleId
    ) {
        return getModules(stack).stream()
                .filter(entry -> entry.id().equals(moduleId))
                .findFirst()
                .orElse(null);
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

        List<GhostModuleData.Entry> modules =
                new ArrayList<>(getModules(stack));

        modules.removeIf(entry ->
                entry.id().equals(moduleId)
        );

        if (modules.isEmpty()) {
            stack.remove(
                    ModDataComponents.GHOST_MODULES.get()
            );
        } else {
            stack.set(
                    ModDataComponents.GHOST_MODULES.get(),
                    List.copyOf(modules)
            );
        }
    }
}