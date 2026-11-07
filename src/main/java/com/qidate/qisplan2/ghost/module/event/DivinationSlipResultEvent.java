package com.qidate.qisplan2.ghost.module.event;

import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;

/** 鬼签签文已经确定这一事实，保证裁决与延迟效果使用同一结果。 */
public record DivinationSlipResultEvent(LivingEntity user, int result) implements GhostEvent {
    public DivinationSlipResultEvent {
        Objects.requireNonNull(user, "user");
        if (result < 0 || result > 2) throw new IllegalArgumentException("Invalid divination result");
    }
}
