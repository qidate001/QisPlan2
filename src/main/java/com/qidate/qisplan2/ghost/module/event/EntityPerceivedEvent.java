package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/**
 * 实体感知事件。
 *
 * observer：观察者。
 * target：被观察到的实体。
 *
 * 只描述感知行为，不规定感知后产生的效果。
 */
public record EntityPerceivedEvent(
        LivingEntity observer,
        LivingEntity target
) implements GhostEvent {

    public EntityPerceivedEvent {
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(target, "target");
    }
}