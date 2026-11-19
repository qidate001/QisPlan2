package com.qidate.qisplan2.death;

/** 灵异实体的交互边界；袭击传递实际残余强度，不再进行防御/停滞时间换算。 */
public interface SupernaturalEntity {
    /** 当前灵异强度。 */
    double getSupernaturalStrength();
    /** 受到一次普通灵异袭击，转换成持续一秒的压制。 */
    void onSupernaturalAttack(double strength);
    /** 是否已无可自主行动的模块。 */
    boolean isSupernaturallyStunned();
}
