package com.qidate.qisplan2.ghost.module.data;

/**
 * 灵异模块的初始数据定义。
 *
 * 该数据描述模块在创建新实例时使用的默认属性，
 * 不代表任何实体或物品当前实际持有的数据。
 *
 * 模块实例创建后，其属性可以独立变化，
 * 不会反向修改这份初始数据定义。
 *
 * 当前仅包含灵异强度和初始复苏值。
 * 沉睡与死机属于独立的实例状态，不属于初始数值属性。
 */
public record GhostModuleInitialData(
        double intensity,
        double revival
) {

    /**
     * 创建初始数据时进行基本校验。
     *
     * 两个属性均要求是非负有限数值。
     */
    public GhostModuleInitialData {
        validate("intensity", intensity);
        validate("revival", revival);
    }

    /**
     * 校验灵异属性数值。
     */
    private static void validate(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(
                    name + " must be a finite non-negative value: " + value
            );
        }
    }
}