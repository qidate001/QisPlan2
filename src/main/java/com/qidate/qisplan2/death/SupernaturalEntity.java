package com.qidate.qisplan2.death;

/** 灵异实体的只读交互边界；灵异效果通过通用世界端口执行。 */
public interface SupernaturalEntity {
    /** 当前灵异强度。 */
    double getSupernaturalStrength();
    /** 是否已无可自主行动的模块。 */
    boolean isSupernaturallyStunned();
}
