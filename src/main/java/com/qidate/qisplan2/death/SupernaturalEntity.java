package com.qidate.qisplan2.death;

public interface SupernaturalEntity {

    /**
     * 受到灵异攻击时调用。
     *
     * @param ticks 停滞时间
     */
    void onSupernaturalAttack(int ticks);
}