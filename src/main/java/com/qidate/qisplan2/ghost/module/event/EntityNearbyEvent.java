package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;

/** 附近存在某个存活实体的事实；自身来源不作为事件参数暴露 */
public record EntityNearbyEvent(LivingEntity target) implements GhostEvent {
    public EntityNearbyEvent {
        Objects.requireNonNull(target, "target");
    }
}
