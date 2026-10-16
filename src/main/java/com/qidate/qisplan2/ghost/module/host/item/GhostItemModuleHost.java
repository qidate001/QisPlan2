package com.qidate.qisplan2.ghost.module.host.item;

import com.qidate.qisplan2.ghost.module.GhostModuleCustomDataModifier;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
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
 * 仅在当前操作期间使用，不长期保存模块容器。
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
     */
    @Override
    public boolean addModule(
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer();

        if (!container.addModule(moduleId, intensity)) {
            return false;
        }

        saveContainer(container);
        return true;
    }

    /**
     * 移除指定模块及其全部实例数据。
     */
    @Override
    public boolean removeModule(ResourceLocation moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer();

        if (!container.removeModule(moduleId)) {
            return false;
        }

        saveContainer(container);
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

        GhostModuleContainer container = createContainer();

        if (!container.setIntensity(moduleId, intensity)) {
            return false;
        }

        saveContainer(container);
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

        GhostModuleContainer container = createContainer();

        if (!container.setRevival(moduleId, revival)) {
            return false;
        }

        saveContainer(container);
        return true;
    }

    /**
     * 修改模块沉睡状态，保留其他属性。
     */
    @Override
    public boolean setSleeping(
            ResourceLocation moduleId,
            boolean sleeping
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer();

        if (!container.setSleeping(moduleId, sleeping)) {
            return false;
        }

        saveContainer(container);
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

        GhostModuleContainer container = createContainer();

        return container.getCustomData(moduleId);
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

        GhostModuleContainer container = createContainer();

        if (!container.setCustomData(moduleId, customData)) {
            return false;
        }

        saveContainer(container);
        return true;
    }

    /**
     * 从 ItemStack 当前的数据组件创建临时容器。
     */
    private GhostModuleContainer createContainer() {
        GhostModuleContainer container = new GhostModuleContainer();
        container.loadEntries(GhostItemData.getModules(stack));
        return container;
    }

    /**
     * 将容器中的全部模块数据一次性写回 ItemStack。
     */
    private void saveContainer(GhostModuleContainer container) {
        GhostItemData.setModules(stack, container.getModules());
    }
}