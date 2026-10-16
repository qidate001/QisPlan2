
package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 灵异模块定义。
 *
 * 模块负责声明自身的身份、初始属性、
 * 自然复苏规则、规则定义以及能力定义。
 *
 * 初始属性只用于创建新的模块实例，
 * 不代表已经存在的实例当前保存的属性。
 *
 * 自然复苏速度与复苏值上限属于模块定义，
 * 不属于模块实例的当前状态。
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
     */
    default GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                1.0D,
                0.0D
        );
    }

    /**
     * 获取自然复苏速度。
     *
     * 单位：复苏值/秒。
     *
     * 默认每秒增加 1 点复苏值。
     * 返回 0 表示默认不进行自然复苏。
     *
     * 具体的自然复苏条件由状态运行机制决定。
     */
    default double getNaturalRevivalRate() {
        return 1.0D;
    }

    /**
     * 获取复苏值上限。
     *
     * 默认上限为 100。
     *
     * 状态运行机制负责校验该定义并限制实例的复苏值。
     */
    default double getMaxRevival() {
        return 100.0D;
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
