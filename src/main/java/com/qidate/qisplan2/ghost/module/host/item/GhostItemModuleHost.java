
package com.qidate.qisplan2.ghost.module.host.item;

import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * ItemStack 的模块宿主适配器。
 *
 * 仅在当前操作期间使用，不长期保存 ItemStack 引用。
 */
public final class GhostItemModuleHost implements GhostModuleDataModifier {

    private final ItemStack stack;

    public GhostItemModuleHost(ItemStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack");
    }

    @Override
    public List<GhostModuleData.Entry> getModules() {
        return GhostItemData.getModules(stack);
    }

    @Override
    public boolean addModule(ResourceLocation moduleId, double intensity) {
        if (!isValidIntensity(intensity)) {
            return false;
        }

        List<GhostModuleData.Entry> current = getModules();

        boolean unchanged = current.stream()
                .anyMatch(entry ->
                        entry.id().equals(moduleId)
                                && Double.compare(entry.intensity(), intensity) == 0
                );

        if (unchanged) {
            return false;
        }

        GhostItemData.addModule(stack, moduleId, intensity);
        return true;
    }

    @Override
    public boolean removeModule(ResourceLocation moduleId) {
        if (!hasModule(moduleId)) {
            return false;
        }

        GhostItemData.removeModule(stack, moduleId);
        return true;
    }

    @Override
    public boolean setIntensity(ResourceLocation moduleId, double intensity) {
        if (!isValidIntensity(intensity)) {
            return false;
        }

        List<GhostModuleData.Entry> current = getModules();

        boolean exists = current.stream()
                .anyMatch(entry -> entry.id().equals(moduleId));

        if (!exists) {
            return false;
        }

        boolean unchanged = current.stream()
                .anyMatch(entry ->
                        entry.id().equals(moduleId)
                                && Double.compare(entry.intensity(), intensity) == 0
                );

        if (unchanged) {
            return false;
        }

        GhostItemData.addModule(stack, moduleId, intensity);
        return true;
    }

    private static boolean isValidIntensity(double intensity) {
        return Double.isFinite(intensity) && intensity >= 0.0D;
    }
}
