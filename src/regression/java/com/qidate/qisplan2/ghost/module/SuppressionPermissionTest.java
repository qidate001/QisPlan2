package com.qidate.qisplan2.ghost.module;

import java.util.Set;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.module.influence.*;
import com.qidate.qisplan2.ghost.module.suppression.*;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** 验证无宿主身份的模块调用与压制权限边界，不启动游戏窗口。 */
public final class SuppressionPermissionTest {
    private static final ResourceLocation TEST = ResourceLocation.parse("qisplan2_test:suppressed");
    private static final ResourceLocation ABILITY = ResourceLocation.parse("qisplan2_test:suppressed/use");
    private record Fact() implements GhostEvent {}
    private static int rules, effects, lifecycle, defenses;
    private static final class Host implements GhostModuleHost {
        final ResourceLocation id;
        boolean controlled, deadlocked;
        GhostSuppressionState suppression = GhostSuppressionState.NONE;
        Host(ResourceLocation id) { this.id = id; }
        public List<GhostModuleData.Entry> getModules() {
            return List.of(new GhostModuleData.Entry(id, 100, 0, false, deadlocked, new CompoundTag()));
        }
        public boolean allowsAbilityRevivalStimulation() { return false; }
        public boolean controlsIntrinsicModules() { return controlled; }
        public GhostSuppressionState suppression(ResourceLocation ignored) { return suppression; }
    }
    /** 断言失败终止构建。 */
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    /** 创建保持同一模块的能力上下文。 */
    private static GhostAbilityContext context(Host host, GhostAbilityContext.Source source) {
        return new GhostAbilityContext(host, host.id, ABILITY, source, null);
    }
    /** 无解压制不参与算术；基础/高度严格大于，完全压制包含1.5倍边界。 */
    private static void thresholds() {
        check(new GhostSuppression(GhostSuppression.Kind.NORMAL, 100).level(100) == GhostSuppression.Level.NONE, "等强不压制");
        check(new GhostSuppression(GhostSuppression.Kind.NORMAL, 130).level(100) == GhostSuppression.Level.BASIC, "130仍为基础");
        check(new GhostSuppression(GhostSuppression.Kind.NORMAL, 150).level(100) == GhostSuppression.Level.COMPLETE, "150进入完全压制");
        check(new GhostSuppression(GhostSuppression.Kind.NORMAL, 151).level(100) == GhostSuppression.Level.COMPLETE, "151完全压制");
        check(GhostSuppression.absolute().level(Double.MAX_VALUE) == GhostSuppression.Level.COMPLETE, "无解不比较强度");
    }
    /** 验证规则、行为、主动和持续能力分别走正确权限，体内豁免不会覆盖外来压制。 */
    private static void permissions() {
        GhostModuleRegistry.register(new GhostModule() {
            public ResourceLocation getId() { return TEST; }
            public List<GhostAbility> getIntrinsicAbilities() {
                return List.of(new DefinedGhostAbility(ABILITY, ResourceLocation.parse("minecraft:textures/item/stick.png"),
                        c -> true, c -> { effects++; return true; }));
            }
            public Set<Class<? extends GhostEvent>> abilityEventTypes() { return Set.of(GhostEvent.class); }

    /** 仅接收已声明订阅的持续能力事实。 */
    public void onAbilityEvent(GhostModuleContext c, GhostEvent event) { lifecycle++; }
            public List<GhostAttackResponse> getAttackResponses() { return List.of((module, attack) -> defenses++); }
            public List<GhostRule> getRules() { return List.of(new GhostRule() {
                public ResourceLocation getId() { return TEST; }
                public Set<Class<? extends GhostEvent>> eventTypes() { return Set.of(Fact.class); }

        /** 已由订阅索引定位；此处只判断事件携带的事实。 */
        public boolean matches(GhostEvent event) { return event instanceof Fact; }
                public boolean canTrigger(GhostRuleContext c, GhostEvent event) { return true; }
                public void onTrigger(GhostRuleContext c, GhostEvent event) { rules++; }
            }); }
        });
        var host = new Host(TEST);
        host.controlled = true;
        host.suppression = new GhostSuppressionState(GhostSuppression.Level.BASIC, GhostSuppression.Level.NONE);
        check(!context(host, GhostAbilityContext.Source.BEHAVIOR).isModuleAvailable(), "基础阻止行动");
        check(GhostAbilityExecutor.tryExecuteWithResult(context(host, GhostAbilityContext.Source.POSSESSION)), "基础允许主动");
        GhostModuleRuntime.dispatch(host, new Fact());
        check(rules == 1, "基础仍可触发规则");
        host.suppression = new GhostSuppressionState(GhostSuppression.Level.HIGH, GhostSuppression.Level.NONE);
        GhostModuleRuntime.dispatch(host, new Fact());
        check(rules == 1, "高度阻止规则");
        check(GhostAbilityExecutor.tryExecuteWithResult(context(host, GhostAbilityContext.Source.POSSESSION)), "高度允许主动");
        host.suppression = new GhostSuppressionState(GhostSuppression.Level.COMPLETE, GhostSuppression.Level.NONE);
        int before = lifecycle;
        GhostModuleRuntime.dispatch(host, new Fact());
        check(lifecycle == before, "完全压制不能运行持续生命周期");
        for (var source : GhostAbilityContext.Source.values())
            check(!GhostAbilityExecutor.tryExecuteWithResult(context(host, source)), "外来完全压制禁止所有调用渠道");
        GhostAttackResponses.apply(GhostAttackResponses.collect(List.of(host)),
                new GhostAttackContext(new SupernaturalAttackEvent(UUID.randomUUID(), 10), null, () -> {}, () -> 0));
        check(defenses == 0, "完全压制不能通过防护声明绕过能力限制");
        host.suppression = new GhostSuppressionState(GhostSuppression.Level.NONE, GhostSuppression.Level.COMPLETE);
        check(GhostAbilityExecutor.tryExecuteWithResult(context(host, GhostAbilityContext.Source.POSSESSION)), "体内压制允许驭鬼主动调用");
        host.controlled = false;
        check(!GhostAbilityExecutor.tryExecuteWithResult(context(host, GhostAbilityContext.Source.POSSESSION)), "普通载体不能伪装体内豁免");
        host.suppression = GhostSuppressionState.NONE;
        host.deadlocked = true;
        check(GhostAbilityExecutor.tryExecuteWithResult(context(host, GhostAbilityContext.Source.POSSESSION)), "解除压制后仍保留死机但允许手动调用");
        check(!context(host, GhostAbilityContext.Source.RULE).isModuleAvailable(), "解除压制不得清除原死机状态");
    }
    /** 仅运行通用压制阈值和调用权限边界，不依赖具体厉鬼实现。 */
    public static void run() {
        thresholds(); permissions();
        System.out.println("Suppression thresholds and permission checks passed");
    }
}
