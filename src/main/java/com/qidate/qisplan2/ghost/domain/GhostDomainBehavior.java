package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.ghost.isolation.GhostIsolationSystem;
import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import com.qidate.qisplan2.ghost.layer.GhostResistanceHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Set;

public interface GhostDomainBehavior {

    /**
     * 当鬼域创建时调用。
     *
     * <p>
     * 用于执行该鬼域行为自身的初始化逻辑。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 创建完成的鬼域
     */
    default void onCreate(
            ServerLevel level,
            GhostDomain domain
    ) {
    }

    /**
     * 获取该鬼域行为的周期执行间隔。
     *
     * <p>
     * 返回值表示多少个服务器 Tick 执行一次
     * {@link #onTick(ServerLevel, GhostDomain, Set)}。
     * </p>
     *
     * <p>
     * 返回小于等于 0 的值表示该鬼域行为
     * 不需要周期性 Tick。
     * </p>
     *
     * <p>
     * 周期计时由 {@link GhostDomainManager} 统一负责，
     * 具体鬼域行为不应该自行维护 Tick 计数器。
     * </p>
     *
     * @return 周期执行间隔；小于等于 0 表示不需要周期 Tick
     */
    default int getTickInterval() {
        return 0;
    }

    /**
     * 执行鬼域行为的周期性逻辑。
     *
     * <p>
     * 本方法只负责执行行为规则，
     * 不负责寻找或判断哪些实体处于鬼域中。
     * </p>
     *
     * <p>
     * {@code entities} 已经由
     * {@link GhostDomainEntityTracker} 根据服务器权威的
     * 鬼域实体关系提供。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼域
     * @param entities 当前处于该鬼域中的实体
     */
    default void onTick(
            ServerLevel level,
            GhostDomain domain,
            Set<Entity> entities
    ) {
    }

    /**
     * 当鬼域被删除时调用。
     *
     * <p>
     * 用于执行该鬼域行为自身的清理逻辑。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 被删除的鬼域
     */
    default void onRemove(
            ServerLevel level,
            GhostDomain domain
    ) {
    }

    /**
     * 当实体进入鬼域时调用。
     *
     * <p>
     * 默认情况下，实体进入鬼域后，
     * 会根据鬼域层数与实体自身的灵异抵抗，
     * 决定实体实际进入的鬼域层数。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 实体进入的鬼域
     * @param entity 进入鬼域的实体
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
     * 当实体离开鬼域时调用。
     *
     * <p>
     * 默认情况下，实体离开鬼域后，
     * 将其鬼域层数恢复为 0。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 实体离开的鬼域
     * @param entity 离开鬼域的实体
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
     * 当实体最终生效的鬼域发生切换时调用。
     *
     * <p>
     * 实体可能同时处于多个重叠鬼域中。
     * 当最终生效鬼域从 {@code oldDomain}
     * 切换到 {@code newDomain} 时，
     * 由新鬼域行为处理这次切换。
     * </p>
     *
     * @param level 当前服务器维度
     * @param oldDomain 原最终生效鬼域
     * @param newDomain 新最终生效鬼域
     * @param entity 发生切换的实体
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
     * 当实体所在鬼域层数发生变化时调用。
     *
     * <p>
     * 默认情况下，直接将实体的鬼域层数
     * 更新为当前鬼域层数。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼域
     * @param entity 处于鬼域中的实体
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
     * 判断该实体是否应该提升到更高的鬼域层数。
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼域
     * @param entity 处于鬼域中的实体
     * @return 是否应该提升实体的鬼域层数
     */
    default boolean shouldRaiseEntityLayer(
            ServerLevel level,
            GhostDomain domain,
            Entity entity
    ) {
        return false;
    }

    /**
     * 判断实体是否允许进入该鬼域。
     *
     * <p>
     * 默认情况下，处于灵异隔绝区域中的实体
     * 不允许进入鬼域。
     * </p>
     *
     * <p>
     * 实际的实体追踪与进入事件由
     * {@link GhostDomainEntityTracker} 负责。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 目标鬼域
     * @param entity 尝试进入鬼域的实体
     * @return 是否允许实体进入鬼域
     */
    default boolean canEntityEnter(
            ServerLevel level,
            GhostDomain domain,
            Entity entity
    ) {
        return !GhostIsolationSystem.isIsolated(
                level,
                entity.blockPosition()
        );
    }

    /**
     * 判断玩家是否允许执行鬼域传送。
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼域
     * @param player 进行传送的玩家
     * @return 是否允许传送
     */
    default boolean canTeleport(
            ServerLevel level,
            GhostDomain domain,
            ServerPlayer player
    ) {
        return true;
    }

    /**
     * 执行玩家的鬼域传送。
     *
     * <p>
     * 默认实现直接调用 Minecraft 原版玩家传送。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼域
     * @param player 进行传送的玩家
     * @param x 目标 X 坐标
     * @param y 目标 Y 坐标
     * @param z 目标 Z 坐标
     * @return 是否成功执行传送
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

    /**
     * 判断该鬼域是否拥有实体视觉。
     *
     * <p>
     * 如果返回 {@code true}，
     * GhostDomainManager 会为鬼域主人维护实体视觉状态。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼域
     * @return 是否启用实体视觉
     */
    default boolean hasEntityVision(
            ServerLevel level,
            GhostDomain domain
    ) {
        return true;
    }

    /**
     * 获取该鬼域达到多少层后可以解锁飞行能力。
     *
     * <p>
     * 返回小于等于 0 的值表示该鬼域不提供飞行能力。
     * </p>
     *
     * @return 飞行解锁层数
     */
    default int getFlightUnlockLayer() {
        return 2;
    }
}