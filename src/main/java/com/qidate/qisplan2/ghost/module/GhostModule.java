package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 灵异模块定义。
 *
 * 模块负责声明自身的身份、初始属性、
 * 规则定义以及能力定义。
 *
 * 初始属性只用于创建新的模块实例，
 * 不代表已经存在的实例当前保存的属性。
 */
public interface GhostModule {

    /**
     * 获取模块的唯一标识。
     */
    ResourceLocation getId();

    /**
     * 获取模块定义提供的初始属性。
     *
     * 默认值：
     * - 灵异强度：1.0
     * - 复苏值：0.0
     * - 死机值：0.0
     */
    default GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                1.0D,
                0.0D,
                0.0D
        );
    }

    /**
     * 获取此模块定义的规则列表。
     *
     * 默认没有规则，保证现有模块兼容。
     */
    default List<GhostRule> getRules() {
        return List.of();
    }

    /**
     * 获取此模块定义的能力列表。
     *
     * 默认没有能力，保证现有模块兼容。
     */
    default List<GhostAbility> getAbilities() {
        return List.of();
    }

    /**
     * 获取此模块定义的媒介列表。
     *
     * 默认没有媒介，保证现有模块兼容。
     */
    default List<GhostMedium> getMedia() {
        return List.of();
    }
}