package com.qidate.qisplan2.death;

/**
 * 可以参与灵异交互的实体。
 *
 * <p>
 * 提供实体的基础灵异属性，以及受到灵异攻击时的处理接口。
 */
public interface SupernaturalEntity {

    /**
     * 灵异强度。
     *
     * <p>
     * 数值越高，发动灵异攻击时的强度越高。
     */
    double getSupernaturalStrength();

    /**
     * 灵异防御强度。
     *
     * <p>
     * 数值越高，受到灵异攻击后的影响越弱。
     */
    double getSupernaturalDefense();

    /**
     * 普通灵异攻击。
     *
     * @param ticks 死机时间
     */
    void onSupernaturalAttack(int ticks);

    /**
     * 永久灵异攻击。
     *
     * <p>
     * 直接进入永久死机。
     */
    void onPermanentSupernaturalAttack();

    /**
     * 当前是否处于普通或永久死机。
     */
    boolean isSupernaturallyStunned();

    /**
     * 当前是否永久死机。
     */
    boolean isPermanentlySupernaturallyStunned();

    /**
     * 清除普通死机状态。
     *
     * <p>
     * 永久死机不应该被这个方法清除。
     */
    void clearSupernaturalStun();
}