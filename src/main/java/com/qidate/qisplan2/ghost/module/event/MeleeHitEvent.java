package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;

/** 服务端确认一次直接近战命中。不同于点击或普通接触，不由灵异伤害再次生成。 */
public record MeleeHitEvent(LivingEntity target) implements GhostEvent {
    /** 命中事实必须有明确目标。 */
    public MeleeHitEvent { Objects.requireNonNull(target); }
}
