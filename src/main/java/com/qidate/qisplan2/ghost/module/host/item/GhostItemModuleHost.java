package com.qidate.qisplan2.ghost.module.host.item;

import com.qidate.qisplan2.ghost.module.GhostModuleCustomDataModifier;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

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
    private final Player carrier;

    /**
     * 返回物品当前的携带者，仅供适配层解释位置和暴露状态。
     * 未提供携带者的纯数据访问场景返回 null。
     */
    public Player carrier() { return carrier; }

    /**
     * 返回适配器绑定的物品栈，用于组件、耐久和外观同步。
     */
    public ItemStack stack() { return stack; }

    /**
     * 创建绑定物品栈的数据适配器，可同时提供当前携带者。
     * 未提供携带者时仍可读写模块组件，但不能据此认定物品已手持暴露。
     * 物品栈必须非 null；适配器不复制物品或另建模块状态。
     */
    public GhostItemModuleHost(ItemStack stack) {
        this(stack, null);
    }

    /**
     * 创建绑定物品栈的数据适配器，可同时提供当前携带者。
     * 未提供携带者时仍可读写模块组件，但不能据此认定物品已手持暴露。
     * 物品栈必须非 null；适配器不复制物品或另建模块状态。
     */
    public GhostItemModuleHost(ItemStack stack, Player carrier) {
        this.stack = Objects.requireNonNull(stack, "stack");
        this.carrier = carrier;
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
     * 修改模块死机状态，保留其他属性。
     */
    @Override
    public boolean setDeadlocked(
            ResourceLocation moduleId,
            boolean deadlocked
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleContainer container = createContainer();

        if (!container.setDeadlocked(moduleId, deadlocked)) {
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
     * 保留模块的强度、复苏值、沉睡和死机状态。
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