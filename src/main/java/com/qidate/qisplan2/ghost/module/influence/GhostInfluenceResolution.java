package com.qidate.qisplan2.ghost.module.influence;

/**
 * 单条灵异影响的裁决状态。
 */
public enum GhostInfluenceResolution {

    /**
     * 当前裁决规则允许该影响生效。
     */
    ACCEPTED,

    /**
     * 被更强的对抗影响压制。
     */
    SUPPRESSED,

    /**
     * 同源、同强度的冲突导致死机。
     */
    DEADLOCKED,

    /**
     * 存在尚未解决的冲突，不允许直接执行效果。
     */
    CONTESTED
}