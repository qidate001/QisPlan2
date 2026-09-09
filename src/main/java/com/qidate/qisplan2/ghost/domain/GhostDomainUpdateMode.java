package com.qidate.qisplan2.ghost.domain;

public enum GhostDomainUpdateMode {

    /**
     * 当鬼域中心移动超过指定距离时自动同步。
     */
    DISTANCE,

    /**
     * 只有鬼域主动调用 updatePosition() 时才同步。
     */
    MANUAL
}