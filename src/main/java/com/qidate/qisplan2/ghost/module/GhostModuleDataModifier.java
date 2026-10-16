package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 灵异模块数据修改接口。
 *
 * 宿主可以是厉鬼实体、灵异物品或其他能够保存模块数据的对象。
 *
 * 此接口只规定如何修改模块实例的数据，
 * 不负责决定模块如何注册、安装或触发。
 */
public interface GhostModuleDataModifier extends GhostModuleHost {

    /**
     * 根据模块定义创建新的模块实例。
     *
     * 初始属性只在模块尚未安装时使用。
     * 如果宿主已经拥有该模块，则不覆盖其当前数据。
     *
     * 新实例默认不处于沉睡状态。
     * 沉睡状态由实例数据容器负责初始化。
     *
     * @param module 模块定义
     * @return 成功创建实例时返回 true，否则返回 false
     */
    default boolean addModule(GhostModule module) {
        Objects.requireNonNull(module, "module");

        ResourceLocation moduleId = Objects.requireNonNull(
                module.getId(),
                "module id"
        );

        // 已有实例必须保留自己的当前属性。
        if (hasModule(moduleId)) {
            return false;
        }

        // 从模块定义取得初始属性，而不是硬编码默认值。
        GhostModuleInitialData initialData = Objects.requireNonNull(
                module.getInitialData(),
                "module initial data"
        );

        // 先创建实例，并写入初始强度。
        if (!addModule(moduleId, initialData.intensity())) {
            return false;
        }

        // 再初始化初始复苏值。
        setRevival(moduleId, initialData.revival());

        return true;
    }

    /**
     * 添加模块。
     *
     * 如果宿主已经拥有该模块，具体实现应明确处理重复添加的情况。
     *
     * @param moduleId 模块 ID
     * @param intensity 初始或更新后的灵异强度
     * @return 数据发生变化时返回 true，否则返回 false
     */
    boolean addModule(ResourceLocation moduleId, double intensity);

    /**
     * 移除指定模块。
     *
     * @param moduleId 模块 ID
     * @return 成功移除时返回 true，否则返回 false
     */
    boolean removeModule(ResourceLocation moduleId);

    /**
     * 修改指定模块的灵异强度。
     *
     * @param moduleId 模块 ID
     * @param intensity 新的灵异强度
     * @return 数据发生变化时返回 true，否则返回 false
     */
    boolean setIntensity(ResourceLocation moduleId, double intensity);

    /**
     * 修改指定模块的复苏值。
     *
     * @param moduleId 模块 ID
     * @param revival 新的复苏值
     * @return 数据发生变化时返回 true，否则返回 false
     */
    boolean setRevival(ResourceLocation moduleId, double revival);

    /**
     * 修改指定模块的沉睡状态。
     *
     * 沉睡状态独立于复苏值：
     * 修改沉睡状态不会自动改变复苏值，
     * 也不会自动决定厉鬼何时苏醒。
     *
     * @param moduleId 模块 ID
     * @param sleeping 是否处于沉睡状态
     * @return 数据发生变化时返回 true，否则返回 false
     */
    boolean setSleeping(ResourceLocation moduleId, boolean sleeping);
}