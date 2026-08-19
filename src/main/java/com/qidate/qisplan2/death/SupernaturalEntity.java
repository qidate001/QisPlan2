package com.qidate.qisplan2.death;

public interface SupernaturalEntity {

    /**
     * 灵异防御强度。
     *
     * 数值越高，受到灵异攻击后的停滞时间越短。
     */
    double getSupernaturalDefense();

    /**
     * 受到灵异攻击后的停滞。
     *
     * @param ticks 停滞时间
     */
    void onSupernaturalAttack(int ticks);
}