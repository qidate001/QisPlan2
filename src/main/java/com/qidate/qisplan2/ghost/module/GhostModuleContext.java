package com.qidate.qisplan2.ghost.module;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 单次模块执行的上下文。
 *
 * 保存当前模块的宿主与模块 ID。
 * 模块属性始终从宿主读取，避免缓存旧值。
 */
public record GhostModuleContext(
        GhostModuleHost host,
        ResourceLocation moduleId
) {

    public GhostModuleContext {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(moduleId, "moduleId");

        if (!host.hasModule(moduleId)) {
            throw new IllegalArgumentException(
                    "Host does not contain module: " + moduleId
            );
        }
    }

    /**
     * 获取当前模块的实时数据。
     */
    public com.qidate.qisplan2.ghost.module.data.GhostModuleData.Entry getData() {
        return host.getModules().stream()
                .filter(entry -> entry.id().equals(moduleId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Module data disappeared: " + moduleId
                ));
    }

    /**
     * 获取当前模块的灵异强度。
     */
    public double intensity() {
        return getData().intensity();
    }

    /**
     * 获取当前模块的复苏值。
     */
    public double revival() {
        return getData().revival();
    }

    /**
     * 获取当前模块的死机值。
     */
    public double shutdown() {
        return getData().shutdown();
    }

    /**
     * 修改当前模块的灵异强度。
     *
     * 只有支持修改数据的宿主才能执行修改。
     */
    public boolean setIntensity(double value) {
        return getModifier().setIntensity(moduleId, value);
    }

    /**
     * 修改当前模块的复苏值。
     */
    public boolean setRevival(double value) {
        return getModifier().setRevival(moduleId, value);
    }

    /**
     * 修改当前模块的死机值。
     */
    public boolean setShutdown(double value) {
        return getModifier().setShutdown(moduleId, value);
    }

    private GhostModuleDataModifier getModifier() {
        if (host instanceof GhostModuleDataModifier modifier) {
            return modifier;
        }

        throw new IllegalStateException(
                "Host does not support module data modification: " + moduleId
        );
    }
}