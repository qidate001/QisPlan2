package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.core.ModDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 灵异物品数据访问接口。
 */
public final class GhostItemData {

    private GhostItemData() {
    }

    /**
     * 获取物品携带的全部厉鬼模块。
     */
    public static List<GhostModuleData.Entry> getModules(
            ItemStack stack
    ) {
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
        return getModules(stack).stream()
                .anyMatch(entry -> entry.id().equals(moduleId));
    }

    /**
     * 添加模块。
     *
     * 如果模块已经存在，则更新它的灵异强度。
     */
    public static void addModule(
            ItemStack stack,
            ResourceLocation moduleId,
            double intensity
    ) {
        List<GhostModuleData.Entry> modules =
                new ArrayList<>(getModules(stack));

        modules.removeIf(entry ->
                entry.id().equals(moduleId)
        );

        modules.add(
                new GhostModuleData.Entry(moduleId, intensity)
        );

        stack.set(
                ModDataComponents.GHOST_MODULES.get(),
                List.copyOf(modules)
        );
    }

    /**
     * 移除指定模块。
     */
    public static void removeModule(
            ItemStack stack,
            ResourceLocation moduleId
    ) {
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