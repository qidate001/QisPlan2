package com.qidate.qisplan2.ghost.module.event;

import java.util.Objects;
import java.util.UUID;

/**
 * 灵异袭击已到达某个目标、尚未结算伤害的事实。
 * 事件不是能力请求；效果成功抵挡后标记结果，由袭击入口停止后续结算。
 * 同一次袭击共用剩余强度；后续保护只处理尚未抵挡的部分。
 */
public final class SupernaturalAttackEvent implements GhostEvent {
    private final UUID target;
    private final double intensity;
    private double remainingIntensity;
    private boolean blocked;

    /** 记录受袭目标及来袭强度；无效数值不能用于购买保护。 */
    public SupernaturalAttackEvent(UUID target, double intensity) {
        this.target = Objects.requireNonNull(target, "target");
        if (!Double.isFinite(intensity) || intensity < 0)
            throw new IllegalArgumentException("Invalid attack intensity");
        this.intensity = intensity;
        this.remainingIntensity = intensity;
    }

    /** 返回世界中的受袭目标标识，不暴露模块的载体对象。 */
    public UUID target() { return target; }

    /** 返回这一次袭击的强度。 */
    public double intensity() { return intensity; }

    /** 返回经过各层保护后尚未被抵挡的强度，后续结算必须使用此值。 */
    public double remainingIntensity() { return remainingIntensity; }

    /**
     * 消耗已成功抵挡的强度；调用方必须先支付代价。
     * 多层保护逐层扣减，不能把强度扣为负数或重新增加。
     */
    public void absorb(double amount) {
        if (!Double.isFinite(amount) || amount < 0)
            throw new IllegalArgumentException("Invalid absorbed intensity");
        if (blocked) return;
        remainingIntensity = Math.max(0, remainingIntensity - amount);
        if (remainingIntensity == 0) block();
    }

    /** 查询是否已有能力完整抵挡本次袭击。 */
    public boolean isBlocked() { return blocked; }

    /** 只有效果成功后才应调用；重复标记不会重新消耗代价。 */
    public void block() { blocked = true; remainingIntensity = 0; }
}
