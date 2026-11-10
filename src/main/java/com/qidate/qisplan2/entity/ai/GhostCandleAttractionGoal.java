package com.qidate.qisplan2.entity.ai;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import com.qidate.qisplan2.ghost.module.GhostModuleCombatState;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** 通用的低优先级红烛吸引行为，补足本身没有游荡 Goal 的厉鬼模块宿主。 */
public final class GhostCandleAttractionGoal extends Goal {
    private final ModularGhostEntity host;
    private Player target;
    public GhostCandleAttractionGoal(ModularGhostEntity host) {
        this.host = host;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean canUse() {
        if (!host.isAlive() || host.isCoffinNailed() || host.isSupernaturallyStunned()
                || host.getModules().stream().noneMatch(GhostModuleCombatState::available)) return false;
        target = GhostAvoidanceSystem.preferredTarget(host, GhostAvoidanceSystem.ATTRACTION_RANGE, p -> true);
        return target != null;
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void tick() {
        host.getLookControl().setLookAt(target, 30, 30);
        if (host.distanceToSqr(target) <= 4) host.getNavigation().stop();
        else if (host.tickCount % 10 == 0) host.getNavigation().moveTo(target, 1);
    }
    @Override public void stop() { host.getNavigation().stop(); target = null; }
}
