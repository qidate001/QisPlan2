package com.qidate.qisplan2.death;

public interface SupernaturalEntity {

    /**
     * 灵异防御强度。
     *
     * 数值越高，受到灵异攻击后的死机时间越短。
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
     * 永久死机不应该被这个方法清除。
     */
    void clearSupernaturalStun();
}