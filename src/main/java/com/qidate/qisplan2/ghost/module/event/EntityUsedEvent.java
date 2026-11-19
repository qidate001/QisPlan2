package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;

/** 使用者已对实体发生交互的事实，不代表必须执行某种能力。 */
public record EntityUsedEvent(LivingEntity user, LivingEntity target) implements GhostEvent {}
