package com.qidate.qisplan2.ghost.possession.manager;

import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;

/**
 * 厉鬼强度系统。
 *
 * 负责：
 *
 * 1. 计算一只鬼当前真正发挥出来的强度。
 * 2. 修改当前强度。
 * 3. 统一处理复苏值对强度发挥比例的影响。
 *
 * 强度本身是 PossessedGhostState 的普通持久状态，
 * 与复苏值一样可以随着游戏过程发生变化。
 *
 * 不再存在“本质强度”概念。
 */
public final class GhostStrengthSystem {

    private GhostStrengthSystem() {
    }


    /*
     * ============================================================
     * 计算当前实际强度
     * ============================================================
     */

    /**
     * 根据当前强度、复苏值和最低发挥比例，
     * 计算这只鬼当前真正发挥出来的强度。
     *
     * 公式：
     *
     * effective =
     * strength *
     * (
     *     minimumRatio
     *     + revival * (1 - minimumRatio)
     * )
     *
     * 例如：
     *
     * 当前强度 = 100
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
            double strength,
            double revival,
            double minimumRatio
    ) {

        /*
         * 强度不能为负数。
         */
        strength =
                Math.max(
                        0.0D,
                        strength
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
         */

        double ratio =
                minimumRatio
                        + revival
                        * (
                        1.0D
                                - minimumRatio
                );


        return strength * ratio;
    }


    /**
     * 根据状态直接计算当前实际强度。
     */
    public static double calculate(
            PossessedGhostState state,
            double minimumRatio
    ) {

        if (state == null) {
            return 0.0D;
        }


        return calculate(
                state.strength(),
                state.revival(),
                minimumRatio
        );
    }


    /*
     * ============================================================
     * 增加强度
     * ============================================================
     */

    /**
     * 增加当前强度。
     *
     * 强度是持久状态。
     *
     * 例如：
     *
     * 当前 10
     * 增长 +2
     * → 12
     */
    public static PossessedGhostState addStrength(
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
                        state.strength()
                                + amount
                );


        return new PossessedGhostState(
                state.revival(),
                newStrength,
                state.shallowStun(),
                state.stunTicks(),
                state.permanentStun(),
                state.lastAbilityUseTick()
        );
    }


    /*
     * ============================================================
     * 设置强度
     * ============================================================
     */

    /**
     * 直接设置当前强度。
     */
    public static PossessedGhostState setStrength(
            PossessedGhostState state,
            double strength
    ) {

        if (state == null) {
            return null;
        }


        return new PossessedGhostState(
                state.revival(),
                Math.max(
                        0.0D,
                        strength
                ),
                state.shallowStun(),
                state.stunTicks(),
                state.permanentStun(),
                state.lastAbilityUseTick()
        );
    }
}