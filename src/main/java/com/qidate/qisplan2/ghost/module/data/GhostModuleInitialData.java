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
 * 后续增加新的灵异属性时，可以在这里统一扩展。
 */
public record GhostModuleInitialData(
        double intensity,
        double revival,
        double shutdown
) {

    /**
     * 创建初始数据时进行基本校验。
     *
     * 当前三个属性均要求是非负有限数值。
     */
    public GhostModuleInitialData {
        validate("intensity", intensity);
        validate("revival", revival);
        validate("shutdown", shutdown);
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