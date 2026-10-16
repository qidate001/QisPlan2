package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 可以承载厉鬼模块的宿主。
 *
 * 定义宿主读取模块数据的能力，以及宿主是否允许
 * 能力刺激修改模块的复苏值。
 *
 * 宿主能够保存模块数据，不代表它必须允许模块状态变化。
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

    /**
     * 判断宿主是否允许能力刺激修改复苏值。
     *
     * 默认允许，以保持现有可变宿主的兼容性。
     * 不可变宿主必须覆盖此方法并返回 false。
     *
     * 此方法只约束能力刺激结算，不代表宿主必然参与
     * 自然复苏，也不代表宿主的其他模块属性可以修改。
     */
    default boolean allowsAbilityRevivalStimulation() {
        return true;
    }
}