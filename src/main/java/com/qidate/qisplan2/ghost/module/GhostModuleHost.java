package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 可以承载厉鬼模块的宿主。
 *
 * 只定义统一读取能力，不要求宿主支持修改。
 */
public interface GhostModuleHost {

    /**
     * 获取宿主携带的全部厉鬼模块。
     */
    List<GhostModuleData.Entry> getModules();

    /**
     * 判断宿主是否携带指定模块。
     */
    default boolean hasModule(ResourceLocation moduleId) {
        return getModules().stream()
                .anyMatch(entry -> entry.id().equals(moduleId));
    }
}