package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.module.implementation.divinationslip.GhostDivinationSlipModule;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/** 真实裁决与声明接口的回归：剩余强度、终止、按需调用、累计保护及作用域隔离。 */
public final class AttackResponseTest {
    private static final ResourceLocation MODULE = ResourceLocation.parse("qisplan2_test:response");
    private static final ResourceLocation ABILITY = ResourceLocation.parse("qisplan2_test:attack");
    private static final UUID TARGET = UUID.randomUUID();
    private static final GhostModuleHost HOST = () -> List.of(new GhostModuleData.Entry(MODULE, 40));

    /** 失败时直接终止检查任务。 */
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }

    /** 构造指定来源的影响，保留真实裁决器的输入校验。 */
    private static GhostInfluence influence(GhostInfluenceType type, double strength, UUID source, UUID target, long batch) {
        return new GhostInfluence(HOST, MODULE, ABILITY, target, type, strength, source, batch);
    }

    /** 验证结果真正到达回调，而不只是裁决器内部局部变量。 */
    private static void residualAndTermination() {
        var source = UUID.randomUUID();
        var other = UUID.randomUUID();
        var session = new GhostInfluenceSession();
        session.submit(influence(GhostInfluenceType.PROTECTION, 30, other, TARGET, session.getBatchId()),
                () -> { throw new AssertionError("被击穿保护不能执行"); });
        double[] seen = {-1};
        session.submitResolved(influence(GhostInfluenceType.ATTACK, 40, source, TARGET, session.getBatchId()),
                result -> {
                    seen[0] = result.effectiveIntensity();
                    check(result.absorbedIntensity() == 30, "应记录30抵挡量");
                    check(GhostInfluenceExecution.resolvedAttack(TARGET).remainingIntensity() == 10,
                            "最终伤害入口必须读取剩余10");
                }, () -> {});
        session.resolveAndApply();
        check(seen[0] == 10, "40击穿30必须以10执行");
        var results = GhostInfluenceResolver.resolveDetailed(List.of(
                influence(GhostInfluenceType.PROTECTION, 30, source, TARGET, 2),
                influence(GhostInfluenceType.ATTACK, 30, source, TARGET, 2),
                influence(GhostInfluenceType.ATTACK, 100, other, TARGET, 2),
                influence(GhostInfluenceType.ATTACK, 100, other, UUID.randomUUID(), 2)));
        check(results.get(1).status() == GhostInfluenceResolution.DEADLOCKED, "同源相当应死机");
        check(results.get(2).status() == GhostInfluenceResolution.SUPPRESSED, "死机后待计算袭击不得默认放行");
        check(results.get(3).status() == GhostInfluenceResolution.ACCEPTED, "不得终止其他目标");
    }

    /** 没有相关响应时不计算累计历史，前层完全抵挡后不扣后层代价。 */
    private static void relevantResponsesOnly() {
        int[] calls = {0};
        var event = new SupernaturalAttackEvent(TARGET, 40);
        var attack = new GhostAttackContext(event, null, () -> {}, () -> { calls[0]++; return 40; });
        GhostAttackResponses.apply(List.of(), attack);
        check(calls[0] == 0 && event.remainingIntensity() == 40, "无关上下文不查询保护历史");
        GhostAttackResponses.apply(List.of(
                new GhostAttackResponses.Response(100, c -> { throw new AssertionError("屏蔽后不能扣费"); }),
                new GhostAttackResponses.Response(0, c -> c.event().block())), attack);
        check(event.isBlocked() && calls[0] == 0, "响应按声明顺序且完整抵挡后停止");
        var partial = new SupernaturalAttackEvent(TARGET, 40);
        GhostAttackResponses.apply(List.of(
                new GhostAttackResponses.Response(100, c -> c.event().absorb(30)),
                new GhostAttackResponses.Response(200, c -> check(c.event().remainingIntensity() == 10, "后层只收到10"))),
                new GhostAttackContext(partial, null, () -> {}, () -> 0));
    }

    /** 生签政策由模块声明，累计只读取一次，同源死机不会错误进入累计分支。 */
    private static void lifeSignPolicy() {
        int[] reads = {0}, deadlocks = {0}, removed = {0};
        var event = new SupernaturalAttackEvent(TARGET, 30);
        var same = new GhostAttackContext(event, GhostDivinationSlipModule.ID,
                () -> deadlocks[0]++, () -> { reads[0]++; return 30; });
        GhostDivinationSlipModule.resolveLifeProtection(same, 30, () -> removed[0]++);
        check(event.isDeadlocked() && deadlocks[0] == 1 && removed[0] == 1 && reads[0] == 0,
                "同源保护应死机并清理，不能再次累计或执行袭击");
        for (int total : new int[]{10,20,30}) {
            var next = new SupernaturalAttackEvent(TARGET, 10);
            var context = new GhostAttackContext(next, MODULE, () -> {}, () -> { reads[0]++; return total; });
            GhostDivinationSlipModule.resolveLifeProtection(context, 30, () -> { throw new AssertionError("异源不得注销"); });
            check(next.isBlocked() == (total < 30), "累计达到保护上限才击穿");
            context.recentIntensity();
        }
        check(reads[0] == 3, "每次袭击最多累计一次");
    }

    /** 同目标嵌套也有独立结算；异常及正常退出都必须恢复或清理外层。 */
    private static void scopes() {
        var influence = influence(GhostInfluenceType.ATTACK, 40, UUID.randomUUID(), TARGET, 3);
        var ten = new GhostInfluenceOutcome(GhostInfluenceResolution.ACCEPTED, 40, 10);
        GhostInfluenceExecution.execute(influence, ten, result -> {
            check(GhostInfluenceExecution.claimResolvedAttack(TARGET).remainingIntensity() == 10, "第一次领取结果");
            check(GhostInfluenceExecution.claimResolvedAttack(TARGET) == null, "第二次伤害不得复用旧结算");
            GhostInfluenceExecution.execute(influence(GhostInfluenceType.PROTECTION, 40,
                    UUID.randomUUID(), TARGET, 4), ten, protective ->
                    check(GhostInfluenceExecution.resolvedAttack(TARGET) == null,
                            "嵌套非袭击效果不得误领外层袭击结果"));
            try {
                GhostInfluenceExecution.execute(influence,
                        new GhostInfluenceOutcome(GhostInfluenceResolution.ACCEPTED, 40, 5), inner -> {
                            check(GhostInfluenceExecution.resolvedAttack(TARGET).remainingIntensity() == 5, "嵌套独立强度");
                            throw new IllegalStateException("test");
                        });
            } catch (IllegalStateException expected) {
                check(GhostInfluenceExecution.resolvedAttack(TARGET).remainingIntensity() == 10, "异常恢复外层");
            }
        });
        check(GhostInfluenceExecution.resolvedAttack(TARGET) == null, "退出不得泄漏上下文");
    }

    /** 执行全部对抗回归，不依赖 Minecraft 窗口。 */
    public static void run() {
        residualAndTermination();
        relevantResponsesOnly();
        lifeSignPolicy();
        scopes();
        System.out.println("Attack response, residual intensity and nested scope checks passed");
    }
}
