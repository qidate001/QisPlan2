package com.qidate.qisplan2.ghost.module.event;

import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 接触事实：瞬时接触、开始接触或持续接触。
 * elapsedTicks 只表示本次新观察到的接触时间，不是能力请求或规则阈值。
 * target 始终是接触到的生命实体；事件不携带来源宿主的物理形态。
 */
public record TouchEvent(LivingEntity target,
                         Phase phase, int elapsedTicks) implements GhostEvent {
    public enum Phase { INSTANT, BEGIN, CONTINUE }

    /** 兼容攻击等瞬时接触入口；一次点击不能虚构一段持续接触时间。 */
    public TouchEvent(LivingEntity target) {
        this(target, Phase.INSTANT, 0);
    }

    /** 校验事实完整性；持续采样必须携带正的实际时间增量。 */
    public TouchEvent {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(phase, "phase");
        if (elapsedTicks < 0 || (phase == Phase.INSTANT && elapsedTicks != 0)
                || (phase != Phase.INSTANT && elapsedTicks == 0))
            throw new IllegalArgumentException("Invalid contact duration");
    }

    /** 创建一 tick 的接触采样，由物理检测层判断是否刚进入接触。 */
    public static TouchEvent sample(LivingEntity target, boolean beginning) {
        return new TouchEvent(target, beginning ? Phase.BEGIN : Phase.CONTINUE, 1);
    }

    /** 判断是否为单次触碰或新接触，用于保持不可触之鬼原有的进入触发语义。 */
    public boolean isBeginning() { return phase != Phase.CONTINUE; }
}
