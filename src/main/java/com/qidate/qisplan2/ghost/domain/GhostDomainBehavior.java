package com.qidate.qisplan2.ghost.domain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

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

    /**
     * 实体进入鬼域时调用。
     */
    default void onEntityEnter(
            ServerLevel level,
            GhostDomain domain,
            Entity entity
    ) {
    }

    /**
     * 实体离开鬼域时调用。
     */
    default void onEntityLeave(
            ServerLevel level,
            GhostDomain domain,
            Entity entity
    ) {
    }

    /**
     * 实体从一个鬼域切换到另一个鬼域时调用。
     */
    default void onEntitySwitch(
            ServerLevel level,
            GhostDomain oldDomain,
            GhostDomain newDomain,
            Entity entity
    ) {
    }

    /**
     * 实体鬼域层数发生改变时调用。
     */
    default void onEntityLayerChange(
            ServerLevel level,
            GhostDomain domain,
            Entity entity
    ) {
    }
}