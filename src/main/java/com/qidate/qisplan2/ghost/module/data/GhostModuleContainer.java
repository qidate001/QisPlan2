package com.qidate.qisplan2.ghost.module.data;

import com.qidate.qisplan2.ghost.bearing.HostComposition;
import com.qidate.qisplan2.ghost.bearing.GhostBearing;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 通用灵异模块数据容器。
 *
 * 负责管理模块条目及其通用属性、自定义数据。
 * 不负责宿主的存档生命周期、网络同步或游戏行为。
 */
public final class GhostModuleContainer {
    private final Supplier<HostComposition> composition;

    /** 默认服务于物品及方块，只有一个万能部位。 */
    public GhostModuleContainer() { this(() -> HostComposition.UNIVERSAL); }

    /** 宿主提供自己的组成；以供应接口绑定，容器不识别宿主类型。 */
    public GhostModuleContainer(Supplier<HostComposition> composition) {
        this.composition = Objects.requireNonNull(composition, "composition");
    }

    /** 新模块加入前进行承载校验，状态更新不占用额外容量。 */
    public boolean canInstall(ResourceLocation id) {
        return GhostBearing.canInstall(composition.get(), getModules(), id);
    }

    /** 原子替换运行时模块列表；失败时原列表完整保留。存档读取仍使用 loadEntries。 */
    public boolean replaceEntries(List<GhostModuleData.Entry> entries) {
        try {
            GhostBearing.validateChange(composition.get(), getModules(), entries);
        } catch (IllegalArgumentException rejected) {
            return false;
        }
        loadEntries(entries);
        return true;
    }


    private final List<GhostModuleData.Entry> modules =
            new ArrayList<>();

    /**
     * 返回模块列表的不可变快照。
     */
    public List<GhostModuleData.Entry> getModules() {
        return List.copyOf(modules);
    }

    /**
     * 查找指定模块。
     */
    public GhostModuleData.Entry getModule(
            ResourceLocation moduleId
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        for (GhostModuleData.Entry entry : modules) {
            if (entry.id().equals(moduleId)) {
                return entry;
            }
        }

        return null;
    }

    public boolean hasModule(ResourceLocation moduleId) {
        return getModule(moduleId) != null;
    }

    /**
     * 添加模块；若已存在，则更新强度并保留其他数据。
     */
    public boolean addModule(
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidValue(intensity) || !canInstall(moduleId)) {
            return false;
        }

        GhostModuleData.Entry current = getModule(moduleId);

        if (current != null
                && Double.compare(
                current.intensity(),
                intensity
        ) == 0) {
            return false;
        }

        GhostModuleData.Entry replacement =
                current == null
                        ? new GhostModuleData.Entry(
                        moduleId,
                        intensity
                )
                        : new GhostModuleData.Entry(
                                moduleId,
                                intensity,
                                current.revival(),
                                current.sleeping(),
                                current.deadlocked(),
                                current.customData()
                        );

        putEntry(replacement);
        return true;
    }

    /**
     * 仅在模块不存在时添加。
     */
    public boolean addModuleIfAbsent(
            ResourceLocation moduleId,
            double intensity
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        if (!isValidValue(intensity)
                || hasModule(moduleId) || !canInstall(moduleId)) {
            return false;
        }

        putEntry(
                new GhostModuleData.Entry(
                        moduleId,
                        intensity
                )
        );

        return true;
    }

    public boolean removeModule(ResourceLocation moduleId) {
        Objects.requireNonNull(moduleId, "moduleId");

        return modules.removeIf(
                entry -> entry.id().equals(moduleId)
        );
    }

    public boolean setIntensity(
            ResourceLocation moduleId,
            double intensity
    ) {
        if (!isValidValue(intensity) || !canInstall(moduleId)) {
            return false;
        }

        GhostModuleData.Entry current = getModule(moduleId);

        if (current == null
                || Double.compare(
                current.intensity(),
                intensity
        ) == 0) {
            return false;
        }

        putEntry(
                new GhostModuleData.Entry(
                        current.id(),
                        intensity,
                        current.revival(),
                        current.sleeping(),
                        current.deadlocked(),
                        current.customData()
                )
        );

        return true;
    }

    public boolean setRevival(
            ResourceLocation moduleId,
            double revival
    ) {
        if (!isValidValue(revival)) {
            return false;
        }

        GhostModuleData.Entry current = getModule(moduleId);

        if (current == null
                || Double.compare(
                current.revival(),
                revival
        ) == 0) {
            return false;
        }

        putEntry(
                new GhostModuleData.Entry(
                        current.id(),
                        current.intensity(),
                        revival,
                        current.sleeping(),
                        current.deadlocked(),
                        current.customData()
                )
        );

        return true;
    }

    /**
     * 设置模块实例的沉睡状态。
     *
     * 沉睡状态独立于复苏值，不会自动改变复苏值。
     */
    public boolean setSleeping(
            ResourceLocation moduleId,
            boolean sleeping
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleData.Entry current = getModule(moduleId);

        if (current == null
                || current.sleeping() == sleeping) {
            return false;
        }

        putEntry(current.withSleeping(sleeping));
        return true;
    }

    /**
     * 设置模块实例的死机状态。
     *
     * 死机状态独立于复苏值和沉睡状态。
     */
    public boolean setDeadlocked(
            ResourceLocation moduleId,
            boolean deadlocked
    ) {
        Objects.requireNonNull(moduleId, "moduleId");

        GhostModuleData.Entry current = getModule(moduleId);

        if (current == null
                || current.deadlocked() == deadlocked) {
            return false;
        }

        putEntry(current.withDeadlocked(deadlocked));
        return true;
    }

    /**
     * 获取模块自定义数据副本。
     * 模块不存在时返回空 Tag，不会自动安装模块。
     */
    public CompoundTag getCustomData(
            ResourceLocation moduleId
    ) {
        GhostModuleData.Entry current = getModule(moduleId);

        return current == null
                ? new CompoundTag()
                : current.customData();
    }

    /**
     * 替换模块自定义数据，并保留其他通用属性。
     */
    public boolean setCustomData(
            ResourceLocation moduleId,
            CompoundTag customData
    ) {
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(customData, "customData");

        GhostModuleData.Entry current = getModule(moduleId);

        if (current == null) {
            return false;
        }

        CompoundTag copy = customData.copy();

        if (current.customData().equals(copy)) {
            return false;
        }

        putEntry(current.withCustomData(copy));
        return true;
    }

    /**
     * 清空当前模块数据。
     */
    public void clear() {
        modules.clear();
    }

    /**
     * 用已有条目恢复容器。
     * 重复 ID 只保留最后出现的条目。
     */
    public void loadEntries(
            List<GhostModuleData.Entry> entries
    ) {
        modules.clear();

        for (GhostModuleData.Entry entry : entries) {
            if (entry == null || entry.id() == null) {
                continue;
            }

            if (!isValidValue(entry.intensity())
                    || !isValidValue(entry.revival())) {
                continue;
            }

            putEntry(
                    new GhostModuleData.Entry(
                            entry.id(),
                            entry.intensity(),
                            entry.revival(),
                            entry.sleeping(),
                            entry.deadlocked(),
                            entry.customData()
                    )
            );
        }
    }

    /** 更新既有条目时保持顺序，承载占位不会因普通数据更新而重新分配。 */
    private void putEntry(GhostModuleData.Entry replacement) {
        for (int index = 0; index < modules.size(); index++) {
            if (modules.get(index).id().equals(replacement.id())) {
                modules.set(index, replacement);
                return;
            }
        }
        modules.add(replacement);
    }

    private static boolean isValidValue(double value) {
        return Double.isFinite(value) && value >= 0.0D;
    }
}