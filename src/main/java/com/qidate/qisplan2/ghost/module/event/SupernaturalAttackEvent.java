package com.qidate.qisplan2.ghost.module.event;

import java.util.Objects;
import java.util.UUID;

/**
 * 灵异袭击已到达某个目标、尚未结算伤害的事实。
 * 事件不是能力请求；效果成功抵挡后标记结果，由袭击入口停止后续结算。
 * 同一次袭击共用一个事件，避免多个保护模块重复支付代价。
 */
public final class SupernaturalAttackEvent implements GhostEvent {
    private final UUID target;
    private final double intensity;
    private boolean blocked;

    /** 记录受袭目标及来袭强度；无效数值不能用于购买保护。 */
    public SupernaturalAttackEvent(UUID target, double intensity) {
        this.target = Objects.requireNonNull(target, "target");
        if (!Double.isFinite(intensity) || intensity < 0)
            throw new IllegalArgumentException("Invalid attack intensity");
        this.intensity = intensity;
    }

    /** 返回世界中的受袭目标标识，不暴露模块的载体对象。 */
    public UUID target() { return target; }

    /** 返回这一次袭击的强度。 */
    public double intensity() { return intensity; }

    /** 查询是否已有能力实际抵挡本次袭击。 */
    public boolean isBlocked() { return blocked; }

    /** 只有效果成功后才应调用；重复标记不会重新消耗代价。 */
    public void block() { blocked = true; }
}
