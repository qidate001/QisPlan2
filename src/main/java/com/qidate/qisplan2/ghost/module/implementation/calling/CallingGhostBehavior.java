package com.qidate.qisplan2.ghost.module.implementation.calling;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.EntityTurnedEvent;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/**
 * 取得移动、朝向控制后才跟随。探测是否可运行不绑定目标，也不修改实体物理状态，
 * 因此强度调度器探测竞争行为时不会抢先产生世界操作。
 */
final class CallingGhostBehavior extends Goal {
    private final ModularGhostEntity host;
    private boolean previousNoPhysics;
    private boolean previousNoGravity;
    CallingGhostBehavior(ModularGhostEntity host) {
        this.host = host;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean canUse() {
        return CallingGhostModule.isActive(host) && CallingGhostTargets.available(host) != null;
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() {
        previousNoPhysics = host.noPhysics;
        previousNoGravity = host.isNoGravity();
        var data = CallingGhostState.read(host);
        data.putBoolean("physicsOwned", true);
        data.putBoolean("previousNoPhysics", previousNoPhysics);
        data.putBoolean("previousNoGravity", previousNoGravity);
        // 被其他模块抢占期间的转身不计入本次跟随，重新建立观察基线。
        data.putBoolean("hasLastPlayerRotation", false);
        CallingGhostState.write(host, data);
        host.noPhysics = true;
        host.setNoGravity(true);
        host.getNavigation().stop();
    }
    @Override public void stop() {
        host.noPhysics = previousNoPhysics;
        host.setNoGravity(previousNoGravity);
        host.setDeltaMovement(Vec3.ZERO);
        host.getNavigation().stop();
        if (host.hasGhostModule(CallingGhostModule.ID)) {
            var data = CallingGhostState.read(host);
            data.putBoolean("physicsOwned", false);
            data.putBoolean("hasLastPlayerRotation", false);
            CallingGhostState.write(host, data);
        }
    }
    @Override public void tick() {
        var player = CallingGhostTargets.available(host);
        if (player == null || !CallingGhostModule.isActive(host)) return;
        var data = CallingGhostState.read(host);
        if (!data.hasUUID("target") || !data.getUUID("target").equals(player.getUUID())) {
            CallingGhostState.bind(host, player);
            data = CallingGhostState.read(host);
        }
        float previous = data.getFloat("lastPlayerYRot");
        float current = player.getYRot();
        boolean observed = data.getBoolean("hasLastPlayerRotation");
        data.putFloat("lastPlayerYRot", current);
        data.putBoolean("hasLastPlayerRotation", true);
        CallingGhostState.write(host, data);
        if (observed && previous != current) {
            GhostModuleRuntime.dispatch(host, new EntityTurnedEvent(host, player, previous, current));
            if (CallingGhostModule.isTurnAround(previous, current)) {
                // 无论袭击是否被保护挡住，本轮遭遇均结束，保持原有目标避让逻辑。
                CallingGhostState.finishAttack(host, player);
                host.setDeltaMovement(Vec3.ZERO);
                return;
            }
        }
        CallingGhostModule.followAndCall(host);
    }
}
