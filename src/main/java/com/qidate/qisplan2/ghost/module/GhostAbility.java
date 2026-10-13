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
}