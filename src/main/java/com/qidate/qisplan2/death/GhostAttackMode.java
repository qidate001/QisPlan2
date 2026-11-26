package com.qidate.qisplan2.death;

/** 灵异效果意图；能力声明与最终世界执行必须采用同一自动路由。 */
public enum GhostAttackMode {
    ATTACK, SUPPRESSION, AUTO;

    /** 只解析 AUTO，显式袭击不会因为目标承载厉鬼而偷偷转为压制。 */
    public GhostAttackMode resolve(boolean carriesGhost) {
        return this == AUTO ? (carriesGhost ? SUPPRESSION : ATTACK) : this;
    }
}
