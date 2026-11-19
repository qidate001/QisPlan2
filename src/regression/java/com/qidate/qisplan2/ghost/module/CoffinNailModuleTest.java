package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.environment.GhostAttachmentAccess;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.module.host.attachment.GhostAttachmentRecord;
import com.qidate.qisplan2.ghost.module.implementation.CoffinNailModule;
import com.qidate.qisplan2.ghost.module.influence.*;
import com.qidate.qisplan2.ghost.module.suppression.*;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** 验证无宿主身份的模块调用、权限边界与附着记录，不启动游戏窗口。 */
public final class CoffinNailModuleTest {
    private static final ResourceLocation TEST = ResourceLocation.parse("qisplan2_test:suppressed");
    private static final ResourceLocation ABILITY = ResourceLocation.parse("qisplan2_test:suppressed/use");
    private record Fact() implements GhostEvent {}
    private static int rules, effects, lifecycle, defenses;
    private static final class Host implements GhostModuleHost {
        final ResourceLocation id;
        boolean controlled, deadlocked, allowed = true, embedded;
        int insertions;
        GhostSuppressionState suppression = GhostSuppressionState.NONE;
        Host(ResourceLocation id) { this.id = id; }
        public List<GhostModuleData.Entry> getModules() {
            return List.of(new GhostModuleData.Entry(id, 100, 0, false, deadlocked, new CompoundTag()));
        }
        public boolean allowsAbilityRevivalStimulation() { return false; }
        public boolean controlsIntrinsicModules() { return controlled; }
        public GhostSuppressionState suppression(ResourceLocation ignored) { return suppression; }
        public GhostWorldAccess moduleWorld(ResourceLocation ignored) {
            var access = new GhostAttachmentAccess() {
                public boolean canEmbed(LivingEntity target) { return allowed && !embedded; }
                public boolean embed(LivingEntity target) {
                    check(canEmbed(target), "不得绕过媒介插入"); embedded = true; insertions++; return true;
                }
                public boolean embedded() { return embedded; }
            };
            return (GhostWorldAccess) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{GhostWorldAccess.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "attachments" -> access;
                        case "aimedTarget" -> null;
                        default -> {
                            if (method.isDefault()) yield InvocationHandler.invokeDefault(proxy, method, args);
                            throw new AssertionError("模块试图读取具体载体: " + method.getName());
                        }
                    });
        }
    }
    /** 断言失败终止构建。 */
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    /** 创建保持同一模块的能力上下文。 */
    private static GhostAbilityContext context(Host host, GhostAbilityContext.Source source) {
        return new GhostAbilityContext(host, host.id, host.id.equals(TEST) ? ABILITY : CoffinNailModule.EMBED, source, null);
    }
    /** 无解压制不参与算术；普通压制严格采用三个大于阈值。 */
    private static void thresholds() {
        check(new GhostSuppression(GhostSuppression.Kind.NORMAL, 100).level(100) == GhostSuppression.Level.NONE, "等强不压制");
        check(new GhostSuppression(GhostSuppression.Kind.NORMAL, 130).level(100) == GhostSuppression.Level.BASIC, "130仍为基础");
        check(new GhostSuppression(GhostSuppression.Kind.NORMAL, 150).level(100) == GhostSuppression.Level.HIGH, "150仍为高度");
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
            public void onAbilityEvent(GhostModuleContext c, GhostEvent event) { lifecycle++; }
            public List<GhostAttackResponse> getAttackResponses() { return List.of((module, attack) -> defenses++); }
            public List<GhostRule> getRules() { return List.of(new GhostRule() {
                public ResourceLocation getId() { return TEST; }
                public boolean supports(GhostEvent event) { return event instanceof Fact; }
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
        check(!context(host, GhostAbilityContext.Source.RULE).isModuleAvailable(), "原死机状态不得被拔钉清除");
    }
    /** 插入能力仅访问端口，附着后的持续声明不要求规则每 tick 重放。 */
    private static void nail() {
        var module = new CoffinNailModule();
        GhostModuleRegistry.register(module);
        var host = new Host(CoffinNailModule.ID);
        check(module.getSuppressions(new GhostModuleContext(host, host.id)).isEmpty(), "未插入没有压制");
        host.allowed = false;
        check(!GhostAbilityExecutor.tryExecuteWithResult(context(host, GhostAbilityContext.Source.POSSESSION)), "媒介不成立不得插入");
        host.allowed = true;
        host.deadlocked = true;
        check(GhostAbilityExecutor.tryExecuteWithResult(context(host, GhostAbilityContext.Source.POSSESSION)), "死机棺材钉可手动插入");
        check(host.insertions == 1 && host.getModules().size() == 1, "插入由端口执行且不改模块表");
        var relation = module.getSuppressions(new GhostModuleContext(host, host.id));
        check(relation.size() == 1 && relation.getFirst().kind() == GhostSuppression.Kind.ABSOLUTE, "插入媒介维持无解压制");
        check(!GhostAbilityExecutor.tryExecuteWithResult(context(host, GhostAbilityContext.Source.POSSESSION)), "重复插入不得重复消耗");
        host.embedded = false;
        check(module.getSuppressions(new GhostModuleContext(host, host.id)).isEmpty(), "拔出解除媒介关系");
    }
    /** 记录保留来源身份、原组件与回收标记，读取副本不得修改原记录。 */
    private static void persistence() {
        var item = new CompoundTag(); item.putString("id", "qisplan2:coffin_nail");
        var components = new CompoundTag(); components.putString("custom_name", "原来的棺材钉");
        item.put("components", components);
        var id = UUID.randomUUID();
        var record = new GhostAttachmentRecord(id, item, true);
        item.getCompound("components").putString("custom_name", "被修改");
        var restored = GhostAttachmentRecord.load(record.save()).orElseThrow();
        check(restored.instance().equals(id) && restored.recoverable(), "重载必须保留同一个附着来源");
        check(restored.item().getCompound("components").getString("custom_name").equals("原来的棺材钉"), "原物品组件必须保留");
        restored.item().putString("id", "minecraft:air");
        check(restored.item().getString("id").equals("qisplan2:coffin_nail"), "外部不可修改记录");
        check(GhostAttachmentRecord.load(new CompoundTag()).isEmpty(), "损坏记录不得制造压制来源");
        check(!GhostAttachmentRecord.load(new GhostAttachmentRecord(id, record.item(), false).save()).orElseThrow().recoverable(),
                "创造来源不得凭空产生可回收物品");
    }
    /** 运行完整棺材钉与压制接口回归。 */
    public static void run() {
        thresholds(); permissions(); nail(); persistence();
        System.out.println("Coffin nail, independent attachment and suppression permission checks passed");
    }
}
