package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;

/** 宿主观察到某个存活实体处于附近的事实 */
public record EntityNearbyEvent(LivingEntity observer, LivingEntity target) implements GhostEvent {
    public EntityNearbyEvent {
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(target, "target");
    }
}
