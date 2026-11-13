package com.qidate.qisplan2.ghost.module.host.player;

import com.qidate.qisplan2.ghost.bearing.HostComposition;

import com.qidate.qisplan2.ghost.module.GhostModuleCustomDataModifier;
import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;
import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Player 的模块宿主适配器。
 *
 * 通过 PlayerGhostData 读写玩家上的模块实例数据。
 * 仅在当前操作期间使用，不长期保存模块容器。
 *
 * 修改模块数据时，必须保留其他通用属性与专属数据。
 */
public final class PlayerGhostModuleHost
        implements GhostModuleDataModifier, GhostModuleCustomDataModifier {

    private final Player player;

    /** 玩家固定采用二十一个人形承载部位。 */
    @Override public HostComposition composition() { return HostComposition.HUMANOID; }


    public PlayerGhostModuleHost(Player player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    public Player player() { return player; }

    /** 完整实例导入，保留强度、复苏、沉睡、死机和 customData；重复模块拒绝覆盖。 */
    public boolean importModule(GhostModuleData.Entry entry) {
        if (hasModule(entry.id()) || !canInstallModule(entry.id())) return false;
        var entries = new ArrayList<>(getModules());
        entries.add(entry);
        PlayerGhostData.setModules(player, entries);
        return true;
    }

    @Override public boolean equals(Object other) {
        return other instanceof PlayerGhostModuleHost host && host.player == player;
    }
    @Override public int hashCode() { return System.identityHashCode(player); }

    /**
     * 获取玩家当前保存的模块实例数据。
     */
    @Override
    public List<GhostModuleData.Entry> getModules() {
        return PlayerGhostData.getModules(player);
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
     * 从 Player 当前的数据组件创建临时容器。
     */
    private GhostModuleContainer createContainer() {
        GhostModuleContainer container = new GhostModuleContainer(this::composition);
        container.loadEntries(PlayerGhostData.getModules(player));
        return container;
    }

    /**
     * 将容器中的全部模块数据一次性写回 Player。
     */
    private void saveContainer(GhostModuleContainer container) {
        PlayerGhostData.setModules(player, container.getModules());
    }
}