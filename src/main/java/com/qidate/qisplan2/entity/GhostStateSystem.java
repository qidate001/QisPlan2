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
 * </ul>
 *
 * <p>
 * 这里不负责具体厉鬼的特殊规则。
 * 例如某只鬼在什么条件下复苏、
 * 死机多久、如何被棺材钉限制，
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
     * 获取复苏值。
     */
    public static double getRevival(
            AbstractGhostEntity ghost
    ) {

        return ghost.getRevival();
    }

    /**
     * 设置复苏值。
     *
     * <p>
     * 复苏值不会低于 0。
     */
    public static void setRevival(
            AbstractGhostEntity ghost,
            double value
    ) {

        ghost.setRevival(value);
    }

    /**
     * 增加复苏值。
     */
    public static void addRevival(
            AbstractGhostEntity ghost,
            double value
    ) {

        ghost.addRevival(value);
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

        return ghost.getSupernaturalStunTicks();
    }

    /**
     * 设置普通灵异死机时间。
     */
    public static void setSupernaturalStunTicks(
            AbstractGhostEntity ghost,
            int ticks
    ) {

        ghost.setSupernaturalStunTicks(ticks);
    }

    /**
     * 增加普通灵异死机时间。
     */
    public static void addSupernaturalStunTicks(
            AbstractGhostEntity ghost,
            int ticks
    ) {

        ghost.addSupernaturalStunTicks(ticks);
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

        ghost.clearSupernaturalStun();
    }


    /*
     * ============================================================
     * 永久灵异死机
     * ============================================================
     */

    /**
     * 设置永久灵异死机状态。
     */
    public static void setPermanentSupernaturalStun(
            AbstractGhostEntity ghost,
            boolean value
    ) {

        ghost.setPermanentSupernaturalStun(value);
    }

    /**
     * 设置为永久死机。
     */
    public static void permanentlyStun(
            AbstractGhostEntity ghost
    ) {

        ghost.setPermanentSupernaturalStun(true);
    }

    /**
     * 判断是否永久死机。
     */
    public static boolean isPermanentlySupernaturallyStunned(
            AbstractGhostEntity ghost
    ) {

        return ghost.isPermanentlySupernaturallyStunned();
    }

    /**
     * 判断当前是否处于灵异死机状态。
     */
    public static boolean isSupernaturallyStunned(
            AbstractGhostEntity ghost
    ) {

        return ghost.isSupernaturallyStunned();
    }
}