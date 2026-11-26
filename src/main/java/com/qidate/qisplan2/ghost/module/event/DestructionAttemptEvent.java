package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** 摧毁即将发生的统一语义；实际损坏对象和撤销操作由事实适配器管理，不暴露宿主类型。 */
public final class DestructionAttemptEvent implements GhostEvent {
    private final LivingEntity attacker;
    private boolean canceled;

    /** 责任者可为空，例如环境损坏；事件本身不会执行或撤销底层操作。 */
    public DestructionAttemptEvent(@Nullable LivingEntity attacker) { this.attacker = attacker; }

    /** 返回可反击的责任者；无责任者时不能凭空选择目标。 */
    @Nullable public LivingEntity getAttacker() { return attacker; }

    /** 请求阻止本次摧毁，适配器在分发结束后落实到真实操作。 */
    public void cancel() { canceled = true; }

    /** 查询最终是否应阻止摧毁。 */
    public boolean isCanceled() { return canceled; }
}
