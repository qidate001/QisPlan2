package com.qidate.qisplan2.ghost.module.implementation.calling;
import com.qidate.qisplan2.ghost.module.host.entity.CallingGhostEntityBinding;

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
public final class CallingGhostBehavior extends Goal {
    private final ModularGhostEntity host;
    private boolean previousNoPhysics;
    private boolean previousNoGravity;

    /**
     * 绑定当前实体并声明移动与视线控制需求。
     * 构造阶段不修改实体物理状态，取得执行权后才在 start 中接管。
     */
    public CallingGhostBehavior(ModularGhostEntity host) {
        this.host = host;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * 判断当前行为是否可以开始执行。
     * 检查行为需要的模块状态、环境或目标条件，交由 Goal 调度器决定是否取得控制。
     */
    @Override public boolean canUse() {
        return CallingGhostEntityBinding.isActive(host) && CallingGhostTargets.available(host) != null;
    }

    /**
     * 重新检查行为是否仍可继续。
     * 条件不满足时由调度器停止行为，释放占用的控制资源。
     */
    @Override public boolean canContinueToUse() { return canUse(); }

    /**
     * 声明此行为需要每 tick 更新，避免目标观察或动作推进被默认调度间隔跳过。
     */
    @Override public boolean requiresUpdateEveryTick() { return true; }

    /**
     * 取得移动控制后保存旧物理状态，开启悬浮跟随并重建转身观察基线。
     * 不会把被其他模块抢占期间的朝向变化计入本轮跟随。
     */
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

    /**
     * 恢复取得控制前的碰撞与重力状态，并停止运动与导航。
     * 清除物理控制标记和观察基线，避免影响共享实体上的其他模块。
     */
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

    /**
     * 更新追踪玩家与朝向观察，发生朝向变化时发布转身事实。
     * 达到转身阈值后结束本轮遭遇，否则通过能力入口继续跟随喊名。
     */
    @Override public void tick() {
        var player = CallingGhostTargets.available(host);
        if (player == null || !CallingGhostEntityBinding.isActive(host)) return;
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
        CallingGhostEntityBinding.followAndCall(host);
    }
}
