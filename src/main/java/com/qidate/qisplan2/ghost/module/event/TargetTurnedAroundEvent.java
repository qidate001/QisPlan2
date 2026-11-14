package com.qidate.qisplan2.ghost.module.event;
import net.minecraft.world.entity.LivingEntity;
/** 被持续观察的目标相对遭遇起始朝向完成回头；这是观察事实，不是释放请求。 */
public record TargetTurnedAroundEvent(LivingEntity target, float initialYaw, float currentYaw) implements GhostEvent {}
