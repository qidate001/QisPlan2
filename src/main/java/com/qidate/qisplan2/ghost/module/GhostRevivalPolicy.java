package com.qidate.qisplan2.ghost.module;

/**
 * 厉鬼模块的复苏刺激结算策略。
 *
 * 负责判断一次复苏刺激是否允许结算。
 *
 * 本接口不负责执行能力、不负责修改复苏值，
 * 也不负责改变模块的沉睡状态或死机状态。
 *
 * 默认策略允许所有合法的复苏刺激结算。
 * 后续接入死机状态时，可以替换或扩展该策略，
 * 使死机期间的能力仍然能够执行，但默认不增长复苏值。
 */
@FunctionalInterface
public interface GhostRevivalPolicy {

    /**
     * 判断本次复苏刺激是否允许结算。
     *
     * @param context 当前能力调用上下文
     * @param stimulation 本次能力声明的复苏刺激
     * @return true 表示允许尝试结算；false 表示跳过本次刺激
     */
    boolean allowsStimulation(
            GhostAbilityContext context,
            GhostAbilityStimulation stimulation
    );

    /**
     * 默认策略。
     *
     * 不额外限制复苏刺激；模块有效性、刺激数值、
     * 最大复苏值等检查仍由原有结算流程负责。
     */
    static GhostRevivalPolicy allowAll() {
        return (context, stimulation) -> true;
    }
}