package com.qidate.qisplan2.entity;

/**
 * 实体厉鬼公共状态系统。
 *
 * <p>
 * 负责管理所有 {@link AbstractGhostEntity}
 * 共有的基础状态。
 *
 * <p>
 * 当前包括：
 *
 * <ul>
 *     <li>复苏值</li>
 *     <li>普通灵异死机</li>
 *     <li>永久灵异死机</li>
 *     <li>棺材钉造成的持续死机</li>
 * </ul>
 *
 * <p>
 * 这里不负责具体厉鬼的特殊规则。
 * 例如某只鬼在什么条件下复苏、
 * 灵异攻击造成多少死机时间，
 * 都应该由对应的实体或上层系统决定。
 */
public final class GhostStateSystem {

    private GhostStateSystem() {
    }


    /*
     * ============================================================
     * 复苏值
     * ============================================================
     */

    /**
     * 获取厉鬼当前复苏值。
     */
    public static double getRevival(
            AbstractGhostEntity ghost
    ) {

        return ghost.getRevivalValue();
    }

    /**
     * 设置厉鬼当前复苏值。
     *
     * <p>
     * 复苏值不会低于 0。
     *
     * @param ghost 厉鬼
     * @param value 新的复苏值
     */
    public static void setRevival(
            AbstractGhostEntity ghost,
            double value
    ) {

        ghost.setRevivalValue(value);
    }

    /**
     * 增加厉鬼当前复苏值。
     *
     * @param ghost 厉鬼
     * @param value 增加的复苏值
     */
    public static void addRevival(
            AbstractGhostEntity ghost,
            double value
    ) {

        setRevival(
                ghost,
                getRevival(ghost) + value
        );
    }


    /*
     * ============================================================
     * 普通灵异死机
     * ============================================================
     */

    /**
     * 获取普通灵异死机剩余时间。
     */
    public static int getSupernaturalStunTicks(
            AbstractGhostEntity ghost
    ) {

        return ghost.getSupernaturalStunTicksValue();
    }

    /**
     * 设置普通灵异死机剩余时间。
     *
     * @param ghost 厉鬼
     * @param ticks 死机时间
     */
    public static void setSupernaturalStunTicks(
            AbstractGhostEntity ghost,
            int ticks
    ) {

        ghost.setSupernaturalStunTicksValue(
                Math.max(0, ticks)
        );
    }

    /**
     * 增加普通灵异死机时间。
     *
     * <p>
     * 结果会限制在
     * {@link Integer#MAX_VALUE}
     * 以内，避免整数溢出。
     *
     * @param ghost 厉鬼
     * @param ticks 增加的死机时间
     */
    public static void addSupernaturalStunTicks(
            AbstractGhostEntity ghost,
            int ticks
    ) {

        long result =
                (long) getSupernaturalStunTicks(ghost)
                        + ticks;

        setSupernaturalStunTicks(
                ghost,
                (int) Math.clamp(
                        result,
                        0L,
                        Integer.MAX_VALUE
                )
        );
    }

    /**
     * 清除普通灵异死机。
     *
     * <p>
     * 不会解除永久死机。
     */
    public static void clearSupernaturalStun(
            AbstractGhostEntity ghost
    ) {

        setSupernaturalStunTicks(
                ghost,
                0
        );
    }


    /*
     * ============================================================
     * 永久灵异死机
     * ============================================================
     */

    /**
     * 设置永久灵异死机状态。
     *
     * <p>
     * 一旦进入永久死机，
     * 普通死机时间会被清零。
     *
     * @param ghost 厉鬼
     * @param value 是否永久死机
     */
    public static void setPermanentSupernaturalStun(
            AbstractGhostEntity ghost,
            boolean value
    ) {

        ghost.setPermanentSupernaturalStunValue(value);

        if (value) {
            setSupernaturalStunTicks(
                    ghost,
                    0
            );
        }
    }

    /**
     * 使厉鬼进入永久死机。
     */
    public static void permanentlyStun(
            AbstractGhostEntity ghost
    ) {

        setPermanentSupernaturalStun(
                ghost,
                true
        );
    }

    /**
     * 判断厉鬼是否永久死机。
     */
    public static boolean isPermanentlySupernaturallyStunned(
            AbstractGhostEntity ghost
    ) {

        return ghost.isPermanentlySupernaturallyStunnedValue();
    }

    /**
     * 判断厉鬼当前是否处于普通或永久死机。
     */
    public static boolean isSupernaturallyStunned(
            AbstractGhostEntity ghost
    ) {

        return isPermanentlySupernaturallyStunned(ghost)
                || getSupernaturalStunTicks(ghost) > 0;
    }


    /*
     * ============================================================
     * Tick
     * ============================================================
     */

    /**
     * 每 tick 更新厉鬼的公共状态。
     *
     * <p>
     * 负责：
     * <ul>
     *     <li>永久死机状态维持</li>
     *     <li>普通死机倒计时</li>
     *     <li>棺材钉造成的持续死机</li>
     * </ul>
     *
     * <p>
     * 该方法返回更新后厉鬼是否仍然处于死机状态。
     * 如果返回 {@code true}，调用方应该跳过本 tick 的普通鬼 AI。
     *
     * @param ghost 厉鬼
     * @return 更新后是否仍然处于死机状态
     */
    public static boolean tick(
            AbstractGhostEntity ghost
    ) {

        /*
         * ========================================================
         * 永久死机
         * ========================================================
         *
         * 永久死机不需要倒计时。
         * 只要状态存在，就始终阻止普通 AI。
         */
        if (isPermanentlySupernaturallyStunned(ghost)) {
            return true;
        }

        /*
         * ========================================================
         * 普通死机倒计时
         * ========================================================
         */
        int stunTicks =
                getSupernaturalStunTicks(ghost);

        if (stunTicks > 0) {
            setSupernaturalStunTicks(
                    ghost,
                    stunTicks - 1
            );
        }

        /*
         * ========================================================
         * 棺材钉
         * ========================================================
         *
         * 棺材钉不是一次性增加 20 tick，
         * 而是每 tick 保持至少 20 tick 的死机状态。
         */
        if (ghost.isCoffinNailed()) {
            setSupernaturalStunTicks(
                    ghost,
                    20
            );
        }

        /*
         * ========================================================
         * 判断最终状态
         * ========================================================
         */
        return isSupernaturallyStunned(ghost);
    }
}