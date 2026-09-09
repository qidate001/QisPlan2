package com.qidate.qisplan2.ghost.domain;

import net.minecraft.server.level.ServerLevel;

public interface GhostDomainBehavior {

    /**
     * 鬼域创建后调用。
     */
    default void onCreate(
            ServerLevel level,
            GhostDomain domain
    ) {
    }

    /**
     * 每个服务器 tick 调用。
     */
    default void tick(
            ServerLevel level,
            GhostDomain domain
    ) {
    }

    /**
     * 鬼域删除前调用。
     */
    default void onRemove(
            ServerLevel level,
            GhostDomain domain
    ) {
    }
}