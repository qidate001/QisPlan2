package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.implementation.UntouchableGhostModule;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 厉鬼模块注册表。
 *
 * 负责将模块 ID 映射到对应的模块实现。
 */
public final class GhostModuleRegistry {

    private static final Map<ResourceLocation, GhostModule> MODULES =
            new LinkedHashMap<>();

    private GhostModuleRegistry() {
    }

    /**
     * 注册一个厉鬼模块。
     *
     * 同一个 ID 不允许重复注册。
     */
    public static void register(GhostModule module) {
        Objects.requireNonNull(module, "module");

        ResourceLocation id = Objects.requireNonNull(
                module.getId(),
                "module id"
        );

        if (MODULES.containsKey(id)) {
            throw new IllegalStateException(
                    "Duplicate ghost module ID: " + id
            );
        }

        MODULES.put(id, module);
    }

    /**
     * 根据 ID 获取模块。
     *
     * 未注册时返回 null。
     */
    public static GhostModule get(ResourceLocation id) {
        return MODULES.get(id);
    }

    /**
     * 判断指定 ID 是否已经注册。
     */
    public static boolean contains(ResourceLocation id) {
        return MODULES.containsKey(id);
    }

    /**
     * 获取所有已注册模块的只读视图。
     */
    public static Collection<GhostModule> getAll() {
        return Collections.unmodifiableCollection(
                MODULES.values()
        );
    }

    /**
     * 注册内置厉鬼模块。
     *
     * 在模组初始化阶段调用一次。
     */
    public static void bootstrap() {
        register(new UntouchableGhostModule());
    }
}