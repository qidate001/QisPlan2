package com.qidate.qisplan2.ghost.module;

import net.minecraft.resources.ResourceLocation;

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
     * 不应影响该模块的复苏值、死机值或其他属性。
     *
     * @param moduleId 模块 ID
     * @param intensity 新的灵异强度
     * @return 数据发生变化时返回 true，否则返回 false
     */
    boolean setIntensity(ResourceLocation moduleId, double intensity);

    /**
     * 修改指定模块的复苏值。
     *
     * 不应影响该模块的灵异强度、死机值或其他属性。
     *
     * @param moduleId 模块 ID
     * @param revival 新的复苏值
     * @return 数据发生变化时返回 true，否则返回 false
     */
    boolean setRevival(ResourceLocation moduleId, double revival);

    /**
     * 修改指定模块的死机值。
     *
     * 不应影响该模块的灵异强度、复苏值或其他属性。
     *
     * @param moduleId 模块 ID
     * @param shutdown 新的死机值
     * @return 数据发生变化时返回 true，否则返回 false
     */
    boolean setShutdown(ResourceLocation moduleId, double shutdown);
}