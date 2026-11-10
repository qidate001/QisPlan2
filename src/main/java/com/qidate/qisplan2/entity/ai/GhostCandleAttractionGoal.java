package com.qidate.qisplan2.entity.ai;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.LivingEntity;

/** 通用的低优先级红烛吸引行为，补足本身没有游荡 Goal 的厉鬼模块宿主。 */
public final class GhostCandleAttractionGoal extends Goal {
    private final ModularGhostEntity host;
    private LivingEntity target;

    /**
     * 为指定实体创建独立的红烛吸引行为，声明占用移动与视线控制。
     * 目标只属于当前行为实例，不在共享模块定义中保存。
     */
    public GhostCandleAttractionGoal(ModularGhostEntity host) {
        this.host = host;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * 判断当前行为是否可以开始执行。
     * 检查行为需要的模块状态、环境或目标条件，交由 Goal 调度器决定是否取得控制。
     */
    @Override public boolean canUse() {
        if (!host.isAlive() || host.isCoffinNailed() || host.isSupernaturallyStunned()
                || host.getModules().stream().noneMatch(GhostModuleCombatState::available)) return false;
        target = GhostAvoidanceSystem.preferredTarget(host, GhostAvoidanceSystem.ATTRACTION_RANGE, p -> true);
        return target != null;
    }

    /**
     * 重新检查行为是否仍可继续。
     * 条件不满足时由调度器停止行为，释放占用的控制资源。
     */
    @Override public boolean canContinueToUse() { return canUse(); }

    /**
     * 推进当前 tick 的行为或待处理任务；具体执行受本方法的有效性检查约束。
     */
    @Override public void tick() {
        host.getLookControl().setLookAt(target, 30, 30);
        if (host.distanceToSqr(target) <= 4) host.getNavigation().stop();
        else if (host.tickCount % 10 == 0) host.getNavigation().moveTo(target, 1);
    }

    /**
     * 停止当前行为，释放移动控制并清理该行为持有的临时目标。
     */
    @Override public void stop() { host.getNavigation().stop(); target = null; }
}
