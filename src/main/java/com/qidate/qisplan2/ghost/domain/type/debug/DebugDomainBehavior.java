package com.qidate.qisplan2.ghost.domain.type.debug;

import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * 调试鬼域行为。
 *
 * <p>
 * 用于测试实体进入鬼域时的事件链是否正常工作。
 * </p>
 */
public final class DebugDomainBehavior
        implements GhostDomainBehavior {

    /**
     * 当实体进入调试鬼域时，
     * 向玩家显示当前已经进入鬼域。
     *
     * <p>
     * 调试行为不需要周期性 Tick，
     * 因此直接使用 Tracker 的实体进入事件。
     * </p>
     */
    @Override
    public void onEntityEnter(
            ServerLevel level,
            GhostDomain domain,
            Entity entity
    ) {

        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        player.displayClientMessage(
                Component.literal(
                        "§5【DEBUG】当前身处调试鬼域中"
                ),
                true
        );
    }
}