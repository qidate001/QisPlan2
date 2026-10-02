package com.qidate.qisplan2.ghost.isolation;

/**
 * 灵异隔绝区域的缓存状态。
 */
public enum IsolationState {

    /**
     * 尚未进行过有效检测。
     */
    UNKNOWN,

    /**
     * 已确认能够隔绝灵异。
     */
    ISOLATED,

    /**
     * 已确认无法隔绝灵异。
     */
    OPEN,

    /**
     * 原有检测结果已经失效，需要重新检测。
     */
    DIRTY
}