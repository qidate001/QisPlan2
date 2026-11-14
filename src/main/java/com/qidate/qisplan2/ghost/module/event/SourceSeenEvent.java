package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;

/** 观察者已经看见事件发出方；具体来源由事件所属模块上下文确定，不暴露宿主。 */
public record SourceSeenEvent(LivingEntity observer) implements GhostEvent {}
