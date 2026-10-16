package com.qidate.qisplan2.ghost.module;

import net.minecraft.resources.ResourceLocation;

/**
 * 灵异能力定义。
 *
 * 能力属于 GhostModule。
 *
 * 能力描述厉鬼能够产生的具体灵异效果，
 * 不依赖某个特定的 GhostEvent 才能执行。
 */
public interface GhostAbility {

    /**
     * 获取能力在所属模块中的唯一标识。
     */
    ResourceLocation getId();

    /**
     * 检查本次能力调用是否满足执行条件。
     *
     * 这里只进行判断，不应产生游戏效果。
     */
    boolean canExecute(GhostAbilityContext context);

    /**
     * 执行能力效果。
     *
     * 调用方应先检查能力是否满足执行条件。
     */
    void execute(GhostAbilityContext context);

    /**
     * 执行能力并返回效果是否成功。
     *
     * 旧能力默认认为执行完成即成功。
     * 需要精确判断效果成功与否的能力可以重写此方法。
     */
    default boolean executeWithResult(GhostAbilityContext context) {
        execute(context);
        return true;
    }

    /**
     * 获取本次能力调用的复苏刺激声明。
     *
     * 默认不产生复苏刺激。
     * 返回的声明只描述刺激数值与结算时机，
     * 不会直接修改模块实例的复苏值。
     */
    default GhostAbilityStimulation getStimulation(
            GhostAbilityContext context
    ) {
        return GhostAbilityStimulation.none();
    }
}