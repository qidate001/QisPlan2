package com.qidate.qisplan2.ghost.module.implementation.nightwanderer;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.EntityNearbyEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.Comparator;
import java.util.EnumSet;

/**
 * 每个宿主独立的夜间行为。Goal 只负责调度，击杀数、计时和目标 UUID
 * 均由模块 customData 承载；不在行为对象内缓存第二份持久状态。
 */
final class NightWandererBehavior extends Goal {
    private final ModularGhostEntity host;
    NightWandererBehavior(ModularGhostEntity host) {
        this.host = host;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
    }
    @Override public boolean canUse() { return NightWandererModule.isActive(host) && !host.level().isDay(); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() { NightWandererModule.activity(host); }
    @Override public void stop() {
        host.getNavigation().stop();
        host.setTarget(null);
        NightWandererMovementSystem.clear(host);
    }

    static boolean validTarget(ModularGhostEntity host, LivingEntity target) {
        return target != null && target != host && target.isAlive() && !target.isRemoved()
                && target.level() == host.level()
                && (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator()
                && host.canDetectPlayer(player) && host.canAttackPlayer(player)));
    }

    static void tickActivity(ModularGhostEntity host) {
        NightWandererState.decrement(host, NightWandererState.ATTACK_COOLDOWN);
        NightWandererState.decrement(host, NightWandererState.TELEPORT_COOLDOWN);

        // 从模块自己的目标记录恢复目标，避免把共享宿主上其他模块的目标当作本模块状态。
        var data = host.getModuleCustomData(NightWandererModule.ID);
        LivingEntity target = null;
        if (data.hasUUID(NightWandererState.TARGET)) {
            var entity = ((ServerLevel)host.level()).getEntity(data.getUUID(NightWandererState.TARGET));
            if (entity instanceof LivingEntity living && validTarget(host, living)) target = living;
        }
        var preferred = GhostAvoidanceSystem.preferredTarget(host, 32, p -> validTarget(host, p));
        if (preferred != null) target = preferred;
        if (target == null) {
            target = host.level().getEntitiesOfClass(LivingEntity.class, host.getBoundingBox().inflate(32),
                    e -> validTarget(host, e) && host.distanceToSqr(e) <= 32 * 32
                            && (e instanceof Player || host.getSensing().hasLineOfSight(e)))
                    .stream().min(Comparator.<LivingEntity>comparingInt(e -> e instanceof Player ? 0 : 1)
                            .thenComparingDouble(host::distanceToSqr)).orElse(null);
        }
        host.setTarget(target);
        NightWandererHuntSystem.tick(host);
        NightWandererMovementSystem.tick(host);
        target = host.getTarget();

        // 猎杀能力可能重新选定目标；读取最新 customData，不能覆盖刚更新的猎杀计时。
        data = host.getModuleCustomData(NightWandererModule.ID);
        if (validTarget(host, target)) data.putUUID(NightWandererState.TARGET, target.getUUID());
        else data.remove(NightWandererState.TARGET);
        host.setModuleCustomData(NightWandererModule.ID, data);

        if (!validTarget(host, target) || NightWandererState.get(host, NightWandererState.ATTACK_COOLDOWN) > 0) {
            host.getNavigation().stop();
            return;
        }
        host.getLookControl().setLookAt(target, 30, 30);
        if (host.distanceToSqr(target) > 9) host.getNavigation().moveTo(target, 1.2D);
        else {
            host.getNavigation().stop();
            // 此处报告目标已处于附近这一事实，袭击由模块规则和裁决决定。
            GhostModuleRuntime.dispatch(host, new EntityNearbyEvent(host, target));
        }
    }
}
