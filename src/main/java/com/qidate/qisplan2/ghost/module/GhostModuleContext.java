package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 单次模块执行的上下文。
 *
 * 保存当前模块的宿主与模块 ID。
 * 模块属性与专属数据均通过宿主接口访问，
 * 避免模块依赖具体宿主类型。
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
    public GhostModuleData.Entry getData() {
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

    /**
     * 获取当前模块的专属数据副本。
     */
    public CompoundTag getCustomData() {
        return getCustomDataModifier().getModuleCustomData(moduleId);
    }

    /**
     * 替换当前模块的全部专属数据。
     */
    public boolean setCustomData(CompoundTag customData) {
        Objects.requireNonNull(customData, "customData");

        return getCustomDataModifier().setModuleCustomData(
                moduleId,
                customData
        );
    }

    /**
     * 读取字符串。
     */
    public String getCustomString(String key, String defaultValue) {
        Objects.requireNonNull(key, "key");

        CompoundTag data = getCustomData();

        return data.contains(key, net.minecraft.nbt.Tag.TAG_STRING)
                ? data.getString(key)
                : defaultValue;
    }

    /**
     * 写入字符串。
     */
    public boolean setCustomString(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        CompoundTag data = getCustomData();
        data.putString(key, value);

        return setCustomData(data);
    }

    /**
     * 读取数值。
     */
    public double getCustomDouble(String key, double defaultValue) {
        Objects.requireNonNull(key, "key");

        CompoundTag data = getCustomData();

        return data.contains(key, net.minecraft.nbt.Tag.TAG_DOUBLE)
                ? data.getDouble(key)
                : defaultValue;
    }

    /**
     * 写入数值。
     */
    public boolean setCustomDouble(String key, double value) {
        Objects.requireNonNull(key, "key");

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Custom data value must be finite: " + value
            );
        }

        CompoundTag data = getCustomData();
        data.putDouble(key, value);

        return setCustomData(data);
    }

    /**
     * 读取布尔值。
     */
    public boolean getCustomBoolean(String key, boolean defaultValue) {
        Objects.requireNonNull(key, "key");

        CompoundTag data = getCustomData();

        return data.contains(key, net.minecraft.nbt.Tag.TAG_BYTE)
                ? data.getBoolean(key)
                : defaultValue;
    }

    /**
     * 写入布尔值。
     */
    public boolean setCustomBoolean(String key, boolean value) {
        Objects.requireNonNull(key, "key");

        CompoundTag data = getCustomData();
        data.putBoolean(key, value);

        return setCustomData(data);
    }

    /**
     * 删除指定专属字段。
     */
    public boolean removeCustomData(String key) {
        Objects.requireNonNull(key, "key");

        CompoundTag data = getCustomData();

        if (!data.contains(key)) {
            return false;
        }

        data.remove(key);

        return setCustomData(data);
    }

    private GhostModuleDataModifier getModifier() {
        if (host instanceof GhostModuleDataModifier modifier) {
            return modifier;
        }

        throw new IllegalStateException(
                "Host does not support module data modification: " + moduleId
        );
    }

    private GhostModuleCustomDataModifier getCustomDataModifier() {
        if (host instanceof GhostModuleCustomDataModifier modifier) {
            return modifier;
        }

        throw new IllegalStateException(
                "Host does not support module custom data: " + moduleId
        );
    }
}