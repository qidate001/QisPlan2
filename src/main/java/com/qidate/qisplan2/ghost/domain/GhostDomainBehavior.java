package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import com.qidate.qisplan2.ghost.layer.GhostResistanceHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

        int enterLayer =
                Math.min(
                        domain.getLayer(),
                        GhostResistanceHandler.getResistance(entity) + 1
                );

        GhostLayerHandler.setLayer(
                entity,
                enterLayer
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

        int enterLayer =
                Math.min(
                        newDomain.getLayer(),
                        GhostResistanceHandler.getResistance(entity) + 1
                );

        GhostLayerHandler.setLayer(
                entity,
                enterLayer
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

    /**
     * 是否允许使用鬼域传送。
     *
     * <p>默认允许。</p>
     */
    default boolean canTeleport(
            ServerLevel level,
            GhostDomain domain,
            ServerPlayer player
    ) {
        return true;
    }

    /**
     * 执行鬼域传送。
     *
     * 默认实现：
     * 传送到目标位置。
     */
    default boolean teleport(
            ServerLevel level,
            GhostDomain domain,
            ServerPlayer player,
            double x,
            double y,
            double z
    ) {

        player.teleportTo(
                level,
                x,
                y,
                z,
                player.getYRot(),
                player.getXRot()
        );

        return true;
    }
}