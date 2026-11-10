package com.qidate.qisplan2.ghost.module.ability;
import net.minecraft.world.entity.LivingEntity;

/** 主动调用提供的目标参数；不是语义事件，也不包含调用载体。 */
public record GhostTargetParameters(LivingEntity target) implements GhostAbilityParameters {}
