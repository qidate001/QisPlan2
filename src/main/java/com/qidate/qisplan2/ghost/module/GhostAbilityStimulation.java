package com.qidate.qisplan2.ghost.module;

/**
 * 灵异能力的复苏刺激声明。
 *
 * 描述能力调用对所属厉鬼模块复苏值产生的影响，
 * 不负责直接修改模块实例状态。
 */
public record GhostAbilityStimulation(
        double revivalAmount,
        Timing timing
) {

    /**
     * 复苏刺激的结算时机。
     */
    public enum Timing {

        /**
         * 能力通过执行条件检查并被接受时结算。
         */
        ON_ACCEPTED,

        /**
         * 能力效果明确成功后结算。
         */
        ON_EFFECT_SUCCESS,

        /**
         * 能力运行到约定的特定阶段后结算。
         */
        MANUAL_CHECKPOINT
    }

    public GhostAbilityStimulation {
        if (!Double.isFinite(revivalAmount)
                || revivalAmount < 0.0D) {
            throw new IllegalArgumentException(
                    "Revival stimulation must be finite and non-negative"
            );
        }

        if (timing == null) {
            throw new NullPointerException("timing");
        }
    }

    /**
     * 不产生复苏刺激。
     */
    public static GhostAbilityStimulation none() {
        return new GhostAbilityStimulation(
                0.0D,
                Timing.ON_ACCEPTED
        );
    }
}