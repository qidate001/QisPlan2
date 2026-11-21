package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;

/** 来源看见了目标的事实；与“目标看见来源”的 SourceSeenEvent 方向相反。 */
public record TargetSeenEvent(LivingEntity target) implements GhostEvent {
    /** 观察事实必须具有真实目标。 */
    public TargetSeenEvent { Objects.requireNonNull(target); }
}
