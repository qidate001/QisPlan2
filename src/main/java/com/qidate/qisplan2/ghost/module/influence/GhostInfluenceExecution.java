package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.curse.CurseManager;

import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.module.host.GhostAttackBridge;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** 同步效果执行作用域。嵌套袭击各自结算，退出或异常时恢复外层，禁止跨线程传递。 */
public final class GhostInfluenceExecution {
    private static final class Frame {
        final UUID target;
        final SupernaturalAttackEvent attack;
        boolean consumed;
        /** 一个声明对应一次最终伤害；同回调再次伤害必须重新结算。 */
        Frame(UUID target, SupernaturalAttackEvent attack) { this.target = target; this.attack = attack; }
    }
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();
    private GhostInfluenceExecution() {}

    /** 把显式影响及目标当前的对抗响应合并，只有仍有有效强度才调用效果。 */
    public static GhostInfluenceOutcome execute(GhostInfluence influence, GhostInfluenceOutcome outcome,
                               Consumer<GhostInfluenceOutcome> effect) {
        var previous = CURRENT.get();
        // 新冲突的保护响应也属于新作用域，不能领取外层尚未执行的伤害结果。
        CURRENT.remove();
        try {
            return executeIsolated(influence, outcome, effect);
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    /** 在已隔离的作用域内收集保护并提交最终效果，阻断分支也由外层统一清理。 */
    private static GhostInfluenceOutcome executeIsolated(GhostInfluence influence, GhostInfluenceOutcome outcome,
            Consumer<GhostInfluenceOutcome> effect) {
        if (influence.type() != GhostInfluenceType.ATTACK && influence.type() != GhostInfluenceType.SUPPRESSION) { effect.accept(outcome); return outcome; }
        if (outcome.effectiveIntensity() <= 0)
            return new GhostInfluenceOutcome(GhostInfluenceResolution.SUPPRESSED, outcome.originalIntensity(), 0);
        var attack = new SupernaturalAttackEvent(influence.targetId(), outcome.effectiveIntensity());
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (var level : server.getAllLevels()) {
                if (level.getEntity(influence.targetId()) instanceof LivingEntity target) {
                    attack = influence.type()==GhostInfluenceType.SUPPRESSION
                            ? CurseManager.resolveSuppression(target,outcome.effectiveIntensity(),influence)
                            : GhostAttackBridge.resolveFresh(target, outcome.effectiveIntensity(), influence);
                    break;
                }
            }
        }
        if (attack.isBlocked()) return new GhostInfluenceOutcome(attack.isDeadlocked()
                ? GhostInfluenceResolution.DEADLOCKED : GhostInfluenceResolution.SUPPRESSED,
                outcome.originalIntensity(), 0);
        CURRENT.set(new Frame(influence.targetId(), attack));
        var finalOutcome = new GhostInfluenceOutcome(outcome.status(), outcome.originalIntensity(), attack.remainingIntensity());
        effect.accept(finalOutcome);
        return finalOutcome;
    }

    /** 最终伤害入口只领取一次，防止同一回调内第二次伤害错误复用已支付的防护。 */
    public static SupernaturalAttackEvent claimResolvedAttack(UUID target) {
        var frame = CURRENT.get();
        if (frame == null || frame.consumed || !frame.target.equals(target)) return null;
        frame.consumed = true;
        return frame.attack;
    }

    /** 同一目标的最终伤害复用已裁决结果，避免寿衣重复付费、生签重复累计。 */
    public static SupernaturalAttackEvent resolvedAttack(UUID target) {
        var frame = CURRENT.get();
        return frame != null && frame.target.equals(target) ? frame.attack : null;
    }
}
