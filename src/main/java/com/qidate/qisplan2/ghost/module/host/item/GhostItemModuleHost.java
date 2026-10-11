package com.qidate.qisplan2.ghost.module.host.item;

import com.qidate.qisplan2.ghost.module.GhostModuleCustomDataModifier;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * ItemStack 的模块宿主适配器。
 *
 * 通过 GhostItemData 读写物品上的模块实例数据。
 * 仅在当前操作期间使用，不长期保存 ItemStack 引用。
 *
 * 修改模块数据时，必须保留其他通用属性与专属数据。
 */
public final class GhostItemModuleHost
        implements GhostModuleDataModifier, GhostModuleCustomDataModifier {

    private final ItemStack stack;

    public GhostItemModuleHost(ItemStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack");
    }

    /**
     * 获取物品当前保存的模块实例数据。
     */
    @Override
    public List<GhostModuleData.Entry> getModules() {
        return GhostItemData.getModules(stack);
    }

    /**
     * 添加模块或更新已有模块的强度。
     *
     * 已有模块的其他数据全部保留。
     */
    @Override
    public boolean addModule(
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidValue(intensity)) {
            return false;
        }

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current != null
                && Double.compare(current.intensity(), intensity) == 0) {
            return false;
        }

        GhostItemData.setModuleEntry(
                stack,
                new GhostModuleData.Entry(
                        moduleId,
                        intensity,
                        current == null ? 0.0D : current.revival(),
                        current == null ? 0.0D : current.shutdown(),
                        current == null
                                ? new CompoundTag()
                                : current.customData()
                )
        );

        return true;
    }

    /**
     * 移除指定模块及其全部实例数据。
     */
    @Override
    public boolean removeModule(ResourceLocation moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (getModuleEntry(moduleId) == null) {
            return false;
        }

        GhostItemData.removeModule(stack, moduleId);
        return true;
    }

    /**
     * 修改模块强度，保留其他数据。
     */
    @Override
    public boolean setIntensity(
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidValue(intensity)) {
            return false;
        }

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current == null
                || Double.compare(current.intensity(), intensity) == 0) {
            return false;
        }

        replaceEntry(
                current,
                new GhostModuleData.Entry(
                        moduleId,
                        intensity,
                        current.revival(),
                        current.shutdown(),
                        current.customData()
                )
        );

        return true;
    }

    /**
     * 修改模块复苏值，保留其他数据。
     */
    @Override
    public boolean setRevival(
            ResourceLocation moduleId,
            double revival
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidValue(revival)) {
            return false;
        }

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current == null
                || Double.compare(current.revival(), revival) == 0) {
            return false;
        }

        replaceEntry(
                current,
                new GhostModuleData.Entry(
                        moduleId,
                        current.intensity(),
                        revival,
                        current.shutdown(),
                        current.customData()
                )
        );

        return true;
    }

    /**
     * 修改模块死机值，保留其他数据。
     */
    @Override
    public boolean setShutdown(
            ResourceLocation moduleId,
            double shutdown
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidValue(shutdown)) {
            return false;
        }

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current == null
                || Double.compare(current.shutdown(), shutdown) == 0) {
            return false;
        }

        replaceEntry(
                current,
                new GhostModuleData.Entry(
                        moduleId,
                        current.intensity(),
                        current.revival(),
                        shutdown,
                        current.customData()
                )
        );

        return true;
    }

    /**
     * 获取指定模块的专属数据副本。
     *
     * 模块不存在时返回空容器，不创建模块。
     */
    @Override
    public CompoundTag getModuleCustomData(ResourceLocation moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        return current == null
                ? new CompoundTag()
                : current.customData();
    }

    /**
     * 替换指定模块的专属数据。
     *
     * 保留模块的强度、复苏值和死机值。
     */
    @Override
    public boolean setModuleCustomData(
            ResourceLocation moduleId,
            CompoundTag customData
    ) {
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(customData, "customData");

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current == null
                || current.customData().equals(customData)) {
            return false;
        }

        replaceEntry(
                current,
                current.withCustomData(customData)
        );

        return true;
    }

    /**
     * 替换完整模块条目。
     */
    private void replaceEntry(
            GhostModuleData.Entry current,
            GhostModuleData.Entry replacement
    ) {
        GhostItemData.setModuleEntry(stack, replacement);
    }

    /**
     * 查找指定模块的完整实例数据。
     */
    private GhostModuleData.Entry getModuleEntry(ResourceLocation moduleId) {
        return getModules().stream()
                .filter(entry -> entry.id().equals(moduleId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 校验模块通用属性数值。
     */
    private static boolean isValidValue(double value) {
        return Double.isFinite(value) && value >= 0.0D;
    }
}