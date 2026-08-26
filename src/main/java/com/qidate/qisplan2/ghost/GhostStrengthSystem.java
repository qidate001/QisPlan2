package com.qidate.qisplan2.ghost;

/**
 * 厉鬼强度系统。
 *
 * 负责：
 *
 * 1. 计算一只鬼当前真正发挥出来的强度。
 * 2. 根据本质强度成长。
 * 3. 统一处理复苏值对强度的影响。
 *
 * 这里暂时只负责基础公式。
 *
 * 后续厉鬼互相压制、增强、环境修正、
 * 协同规则等，都可以继续在这里扩展。
 */
public final class GhostStrengthSystem {

    private GhostStrengthSystem() {
    }


    /**
     * 根据本质强度、复苏值和最低发挥比例，
     * 计算当前实际使用强度。
     *
     * 公式：
     *
     * effective =
     * intrinsic *
     * (
     *     minimumRatio
     *     + revival * (1 - minimumRatio)
     * )
     *
     * 例如：
     *
     * 本质强度 = 100
     * 最低发挥 = 1/3
     *
     * 0% 复苏：
     * 33.3
     *
     * 50% 复苏：
     * 66.7
     *
     * 100% 复苏：
     * 100
     */
    public static double calculate(
            double intrinsicStrength,
            double revival,
            double minimumRatio
    ) {

        /*
         * 本质强度不能为负数。
         */
        intrinsicStrength =
                Math.max(
                        0.0D,
                        intrinsicStrength
                );

        /*
         * 复苏值限制在 0~1。
         */
        revival =
                Math.clamp(
                        revival,
                        0.0D,
                        1.0D
                );

        /*
         * 最低发挥比例限制在 0~1。
         */
        minimumRatio =
                Math.clamp(
                        minimumRatio,
                        0.0D,
                        1.0D
                );

        /*
         * ========================================================
         * 计算当前发挥比例
         * ========================================================
         *
         * 0% 复苏：
         * minimumRatio
         *
         * 100% 复苏：
         * 1.0
         */
        double ratio =
                minimumRatio
                        + revival
                        * (
                        1.0D
                                - minimumRatio
                );

        return intrinsicStrength
                * ratio;
    }


    /**
     * 根据状态和 Ability 的最低发挥比例，
     * 直接计算当前强度。
     */
    public static double calculate(
            PossessedGhostState state,
            double minimumRatio
    ) {

        if (state == null) {
            return 0.0D;
        }

        return calculate(
                state.intrinsicStrength(),
                state.revival(),
                minimumRatio
        );
    }


    /**
     * 增加厉鬼本质强度。
     *
     * 本质强度是成长属性。
     *
     * 例如：
     *
     * 当前 10
     * 增长 +2
     * → 12
     */
    public static PossessedGhostState addIntrinsicStrength(
            PossessedGhostState state,
            double amount
    ) {

        if (state == null) {
            return null;
        }

        if (amount == 0.0D) {
            return state;
        }

        double newStrength =
                Math.max(
                        0.0D,
                        state.intrinsicStrength()
                                + amount
                );

        return new PossessedGhostState(
                state.revival(),
                state.shallowStun(),
                state.stunTicks(),
                state.permanentStun(),
                state.lastAbilityUseTick(),
                newStrength
        );
    }


    /**
     * 将本质强度直接设置为指定值。
     *
     * 后续特殊规则、融合、吞噬等机制
     * 可以使用这个接口。
     */
    public static PossessedGhostState setIntrinsicStrength(
            PossessedGhostState state,
            double strength
    ) {

        if (state == null) {
            return null;
        }

        return new PossessedGhostState(
                state.revival(),
                state.shallowStun(),
                state.stunTicks(),
                state.permanentStun(),
                state.lastAbilityUseTick(),
                Math.max(
                        0.0D,
                        strength
                )
        );
    }
}