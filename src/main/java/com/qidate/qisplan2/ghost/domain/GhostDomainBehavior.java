package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
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
        GhostLayerHandler.setLayer(
                entity,
                domain.getLayer()
        );
    }

    /**
     * 实体离开鬼域时调用。
     */
    default void onEntityLeave(
            ServerLevel level,
            GhostDomain domain,
            Entity entity
    ) {
        GhostLayerHandler.setLayer(
                entity,
                0
        );
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
        GhostLayerHandler.setLayer(
                entity,
                newDomain.getLayer()
        );
    }

    /**
     * 实体鬼域层数发生改变时调用。
     */
    default void onEntityLayerChange(
            ServerLevel level,
            GhostDomain domain,
            Entity entity
    ) {
        GhostLayerHandler.setLayer(
                entity,
                domain.getLayer()
        );
    }

    /**
     * 判断当鬼域总层数增加时，
     * 实体是否应该提升到新的层数。
     *
     * <p>默认不提升。</p>
     */
    default boolean shouldRaiseEntityLayer(
            ServerLevel level,
            GhostDomain domain,
            Entity entity
    ) {
        return false;
    }
}