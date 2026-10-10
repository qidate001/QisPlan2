package com.qidate.qisplan2.ghost.module;

import net.minecraft.resources.ResourceLocation;

/**
 * 可以读取并修改厉鬼模块数据的宿主。
 *
 * 具体的数据存储方式由宿主自身负责。
 */
public interface GhostModuleDataModifier extends GhostModuleHost {

    /**
     * 添加模块。
     *
     * 如果模块已经存在，则更新其灵异强度。
     *
     * @return 数据发生变化时返回 true，否则返回 false
     */
    boolean addModule(ResourceLocation moduleId, double intensity);

    /**
     * 移除指定模块。
     *
     * @return 成功移除模块时返回 true
     */
    boolean removeModule(ResourceLocation moduleId);

    /**
     * 修改指定模块的灵异强度。
     *
     * @return 模块存在且强度发生变化时返回 true
     */
    boolean setIntensity(ResourceLocation moduleId, double intensity);
}
