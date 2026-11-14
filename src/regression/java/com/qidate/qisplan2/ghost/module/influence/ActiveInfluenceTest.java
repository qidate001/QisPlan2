package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.implementation.divinationslip.GhostDivinationSlipModule;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurse;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurseSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

/** 无游戏窗口验证真实裁决会话、持续生签收集和主动执行器的衔接。 */
public final class ActiveInfluenceTest {
    private static final ResourceLocation ID = GhostDivinationSlipModule.ID;
    private static final ResourceLocation ABILITY = ResourceLocation.parse("qisplan2_test:influence");
    private static final UUID TARGET = UUID.randomUUID();
    private static boolean deadlocked, medium;
    private static int effects, cleanup, declarations;

    /** 断言失败直接让 Gradle 检查失败。 */
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }

    /** 动态宿主保留真实默认 carriesModule 行为，仅实现测试需要的数据读写。 */
    private static GhostModuleDataModifier host(ResourceLocation id) {
        return (GhostModuleDataModifier) Proxy.newProxyInstance(ActiveInfluenceTest.class.getClassLoader(),
                new Class<?>[]{GhostModuleDataModifier.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getModules")) return List.of(
                            new GhostModuleData.Entry(id, 30, 0, false, deadlocked, new CompoundTag()));
                    if (method.getName().equals("setDeadlocked")) { deadlocked = (boolean)args[1]; return true; }
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    throw new AssertionError("Unexpected host operation: " + method.getName());
                });
    }

    /** 生签保护和袭击进入同一会话，覆盖来源、强度差异、清理与死机后继续主动使用。 */
    public static void run() {
        GhostModuleRegistry.register(new GhostModule() {
            public ResourceLocation getId() { return ID; }
            public List<GhostAbility> getIntrinsicAbilities() { return List.of(new GhostAbility() {
                public ResourceLocation getId() { return ABILITY; }
                public boolean canExecute(GhostAbilityContext c) { return medium; }
                public GhostAbilityInfluence activeInfluence(GhostAbilityContext c) {
                    declarations++;
                    return new GhostAbilityInfluence(TARGET, GhostInfluenceType.ATTACK, 30, null);
                }
                public void execute(GhostAbilityContext c) { effects++; }
            }); }
        });
        var host = host(ID);
        var context = new GhostAbilityContext(host, ID, ABILITY, GhostAbilityContext.Source.POSSESSION, null);
        var curse = new GhostDivinationCurse(TARGET, new GhostDivinationCurseSource(), 30, 100);
        var session = new GhostInfluenceSession();
        GhostInfluenceCollector.submitCurse(session, host, ID, ABILITY, TARGET, curse, curse, () -> cleanup++);
        GhostInfluenceCollector.submitCurse(session, host, ID, ABILITY, TARGET, curse, curse, () -> cleanup++);
        check(!new GhostAbilityInfluence(TARGET, GhostInfluenceType.ATTACK, 30, null)
                .resolve(session, context, () -> { effects++; return true; }), "同源同强度不得执行死签效果");
        check(deadlocked && effects == 0 && cleanup == 1, "必须写入模块死机且只清理一次生签");
        medium = true;
        check(GhostAbilityExecutor.tryExecuteWithResult(context), "死机后主动能力仍可执行");
        check(effects == 1 && declarations == 1, "主动能力必须经过声明且只执行一次");
        medium = false;
        check(!GhostAbilityExecutor.tryExecuteWithResult(context) && declarations == 1, "媒介/条件不符不得参与裁决");
        check(!GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host, ID, ABILITY,
                GhostAbilityContext.Source.RULE, null)), "死机后的规则调用必须失效");
        deadlocked = false;
        var other = ResourceLocation.parse("qisplan2_test:other");
        GhostModuleRegistry.register(new GhostModule() {
            public ResourceLocation getId() { return other; }
            public List<GhostAbility> getIntrinsicAbilities() { return List.of(new DefinedGhostAbility(ABILITY,
                    ResourceLocation.parse("minecraft:textures/item/flint.png"), c -> true, c -> true)); }
        });
        var otherHost = host(other);
        var otherContext = new GhostAbilityContext(otherHost, other, ABILITY, GhostAbilityContext.Source.POSSESSION, null);
        var otherSession = new GhostInfluenceSession();
        GhostInfluenceCollector.submitCurse(otherSession, otherHost, other, ABILITY, TARGET, curse, curse, () -> cleanup++);
        check(!new GhostAbilityInfluence(TARGET, GhostInfluenceType.ATTACK, 30, null)
                .resolve(otherSession, otherContext, () -> { effects++; return true; }), "异源同强度应被保护抵挡");
        check(!deadlocked && cleanup == 1 && effects == 1, "其他模块不得被生签错误死机或清理保护");
        var weakSession = new GhostInfluenceSession();
        GhostInfluenceCollector.submitCurse(weakSession, host, ID, ABILITY, TARGET, curse, curse, () -> cleanup++);
        check(!new GhostAbilityInfluence(TARGET, GhostInfluenceType.ATTACK, 10, null)
                .resolve(weakSession, context, () -> { effects++; return true; }) && !deadlocked,
                "同源但强度明显不等时应抵挡而非死机");
        System.out.println("Active influence, curse source and deadlock regression checks passed");
    }
}
