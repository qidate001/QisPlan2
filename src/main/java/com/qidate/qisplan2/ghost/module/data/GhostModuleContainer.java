package com.qidate.qisplan2.ghost.module.data;

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

        if (!isValidValue(intensity)) {
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
                        current.shutdown(),
                        current.customData().copy()
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
                || hasModule(moduleId)) {
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
        if (!isValidValue(intensity)) {
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
                        current.shutdown(),
                        current.customData().copy()
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
                || Double.compare(current.revival(), revival) == 0) {
            return false;
        }

        putEntry(
                new GhostModuleData.Entry(
                        current.id(),
                        current.intensity(),
                        revival,
                        current.shutdown(),
                        current.customData().copy()
                )
        );

        return true;
    }

    public boolean setShutdown(
            ResourceLocation moduleId,
            double shutdown
    ) {
        if (!isValidValue(shutdown)) {
            return false;
        }

        GhostModuleData.Entry current = getModule(moduleId);

        if (current == null
                || Double.compare(current.shutdown(), shutdown) == 0) {
            return false;
        }

        putEntry(
                new GhostModuleData.Entry(
                        current.id(),
                        current.intensity(),
                        current.revival(),
                        shutdown,
                        current.customData().copy()
                )
        );

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
                : current.customData().copy();
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
                    || !isValidValue(entry.revival())
                    || !isValidValue(entry.shutdown())) {
                continue;
            }

            putEntry(
                    new GhostModuleData.Entry(
                            entry.id(),
                            entry.intensity(),
                            entry.revival(),
                            entry.shutdown(),
                            entry.customData().copy()
                    )
            );
        }
    }

    private void putEntry(GhostModuleData.Entry replacement) {
        modules.removeIf(
                entry -> entry.id().equals(replacement.id())
        );

        modules.add(replacement);
    }

    private static boolean isValidValue(double value) {
        return Double.isFinite(value) && value >= 0.0D;
    }
}
