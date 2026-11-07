package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;
import java.util.Objects;

/**
 * 鬼域追踪器在本次周期采样中确认：目标正受指定鬼域影响。
 * domainStrength 是采样时鬼域的强度事实；袭击倍率由具体模块的规则决定。
 */
public record GhostDomainExposureEvent(UUID domainId, LivingEntity source,
        LivingEntity target, double domainStrength) implements GhostEvent {
    public GhostDomainExposureEvent {
        Objects.requireNonNull(domainId, "domainId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
    }
}
