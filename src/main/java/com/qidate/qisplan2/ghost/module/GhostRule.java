package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.resources.ResourceLocation;

/**
 * 灵异规则定义。
 *
 * 规则属于 GhostModule。
 *
 * 负责判断语义事件是否满足自身的触发条件，
 * 并在条件满足时执行规则逻辑。
 *
 * 规则不直接等同于能力：
 * 规则负责决定何时触发，能力负责产生具体效果。
 */
public interface GhostRule {

    /**
     * 获取规则在所属模块中的唯一标识。
     */
    ResourceLocation getId();

    /**
     * 判断规则是否关注指定事件。
     *
     * 这里只进行事件类型筛选，
     * 不应在此方法中执行规则效果或修改状态。
     */
    boolean supports(GhostEvent event);

    /**
     * 判断规则当前是否满足触发条件。
     *
     * 此方法应只进行判断，不应产生游戏效果。
     */
    boolean canTrigger(
            GhostRuleContext context,
            GhostEvent event
    );

    /**
     * 执行规则逻辑。
     *
     * 调用方应先完成事件筛选、状态检查和前置条件检查。
     */
    void onTrigger(
            GhostRuleContext context,
            GhostEvent event
    );
}