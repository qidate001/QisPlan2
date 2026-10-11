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
 * 通过 GhostItemData 读写物品上的模块实例数据。
 * 仅在当前操作期间使用，不长期保存 ItemStack 引用。
 *
 * 修改某一项属性时，必须保留同一模块的其他属性，
 * 避免强度、复苏值或死机值被意外重置。
 */
public final class GhostItemModuleHost implements GhostModuleDataModifier {

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
     * 已有模块的复苏值和死机值必须保留。
     */
    @Override
    public boolean addModule(ResourceLocation moduleId, double intensity) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidValue(intensity)) {
            return false;
        }

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        // 如果模块已存在且强度没有变化，则不需要写入。
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
                        current == null ? 0.0D : current.shutdown()
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
     * 修改模块强度，保留复苏值和死机值。
     */
    @Override
    public boolean setIntensity(ResourceLocation moduleId, double intensity) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidValue(intensity)) {
            return false;
        }

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current == null
                || Double.compare(current.intensity(), intensity) == 0) {
            return false;
        }

        GhostItemData.setModuleEntry(
                stack,
                new GhostModuleData.Entry(
                        moduleId,
                        intensity,
                        current.revival(),
                        current.shutdown()
                )
        );

        return true;
    }

    /**
     * 修改模块复苏值，保留强度和死机值。
     */
    @Override
    public boolean setRevival(ResourceLocation moduleId, double revival) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidValue(revival)) {
            return false;
        }

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current == null
                || Double.compare(current.revival(), revival) == 0) {
            return false;
        }

        GhostItemData.setModuleEntry(
                stack,
                new GhostModuleData.Entry(
                        moduleId,
                        current.intensity(),
                        revival,
                        current.shutdown()
                )
        );

        return true;
    }

    /**
     * 修改模块死机值，保留强度和复苏值。
     */
    @Override
    public boolean setShutdown(ResourceLocation moduleId, double shutdown) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidValue(shutdown)) {
            return false;
        }

        GhostModuleData.Entry current = getModuleEntry(moduleId);

        if (current == null
                || Double.compare(current.shutdown(), shutdown) == 0) {
            return false;
        }

        GhostItemData.setModuleEntry(
                stack,
                new GhostModuleData.Entry(
                        moduleId,
                        current.intensity(),
                        current.revival(),
                        shutdown
                )
        );

        return true;
    }

    /**
     * 查找指定模块的完整实例数据。
     *
     * 不存在时返回 null。
     */
    private GhostModuleData.Entry getModuleEntry(ResourceLocation moduleId) {
        return getModules().stream()
                .filter(entry -> entry.id().equals(moduleId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 当前三个灵异数值统一使用非负有限数值规则。
     */
    private static boolean isValidValue(double value) {
        return Double.isFinite(value) && value >= 0.0D;
    }
}