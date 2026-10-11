package com.qidate.qisplan2.ghost.module;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * 灵异模块专属数据修改接口。
 *
 * 宿主负责保存和持久化数据；
 * 模块通过统一接口修改自己的专属数据。
 */
public interface GhostModuleCustomDataModifier {

    /**
     * 获取指定模块实例的专属数据副本。
     */
    CompoundTag getModuleCustomData(ResourceLocation moduleId);

    /**
     * 替换指定模块实例的专属数据。
     */
    boolean setModuleCustomData(
            ResourceLocation moduleId,
            CompoundTag customData
    );
}