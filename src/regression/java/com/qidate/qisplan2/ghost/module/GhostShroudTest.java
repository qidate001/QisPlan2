package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityExecutor;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.environment.GhostEnvironment;
import com.qidate.qisplan2.ghost.module.environment.GhostEnvironmentalEffect;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.module.implementation.GhostShroudModule;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** 运行真实寿衣模块与执行器，以抽象环境验证保护、代价、覆盖与死机语义。 */
public final class GhostShroudTest {
    private static final UUID TARGET = UUID.randomUUID();

    /** 测试无需创建实例。 */
    private GhostShroudTest() {}

    /** 不满足行为约定时给出具体原因。 */
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    /** 模拟覆盖关系，不告诉模块此环境由何种物理对象提供。 */
    private static final class Environment implements GhostEnvironment {
        boolean covered = true;
        /** 返回覆盖状态。 */
        public boolean surfaceCovered() { return covered; }
        /** 只允许保护对应目标。 */
        public boolean coversSurface(UUID target) { return covered && TARGET.equals(target); }
        /** 本测试不提供其他环境媒介。 */
        public boolean exposed() { return false; }
        /** 无燃烧压力。 */
        public double burnPressure() { return 0; }
        /** 无屏蔽声音。 */
        public double blockedSoundIntensity() { return 0; }
        /** 寿衣不应消耗燃料。 */
        public void consume(double amount) { throw new AssertionError("Unexpected fuel cost"); }
        /** 寿衣不应发布鬼烛效果。 */
        public void emit(GhostEnvironmentalEffect effect) { throw new AssertionError("Unexpected candle effect"); }
    }

    /** 完全没有实体、物品或玩家类型的测试宿主。 */
    private static final class Host implements GhostModuleHost, GhostModuleCustomDataModifier {
        final Environment environment = new Environment();
        CompoundTag data = new CompoundTag();
        boolean deadlocked;
        double maximum = 20;
        double strength = 30;
        int paid;
        int drained;

        /** 返回模块当前状态，允许验证死机后的规则和主动调用差异。 */
        public List<GhostModuleData.Entry> getModules() {
            return List.of(new GhostModuleData.Entry(GhostShroudModule.ID, strength, 0, deadlocked, false, data));
        }
        /** 返回独有数据副本，模拟正式存储接口。 */
        public CompoundTag getModuleCustomData(ResourceLocation id) { return data.copy(); }
        /** 保存计时状态。 */
        public boolean setModuleCustomData(ResourceLocation id, CompoundTag value) {
            data = value.copy(); return true;
        }
        /** 由测试环境解释媒介。 */
        public GhostEnvironment moduleEnvironment(ResourceLocation id) { return environment; }
        /** 无复苏写入能力，避免测试依赖玩家存储。 */
        public boolean allowsAbilityRevivalStimulation() { return false; }
        /** 只实现寿衣需要的世界操作；意外依赖其他操作会直接失败。 */
        public GhostWorldAccess moduleWorld(ResourceLocation id) {
            return (GhostWorldAccess) Proxy.newProxyInstance(GhostWorldAccess.class.getClassLoader(),
                    new Class<?>[]{GhostWorldAccess.class}, (proxy, method, args) -> {
                        if (method.getName().equals("consumeMaxHealth")) {
                            double cost = (double) args[1];
                            if (maximum - cost < (double) args[2]) return false;
                            maximum -= cost; paid++; return true;
                        }
                        if (method.getName().equals("drainHealth")) { drained++; return true; }
                        throw new AssertionError("Unexpected world dependency: " + method.getName());
                    });
        }
    }

    /** 创建独立袭击事实，不模拟物理载体。 */
    private static SupernaturalAttackEvent attack(double strength) {
        return new SupernaturalAttackEvent(TARGET, strength);
    }

    /** 检查真实规则分发、多个保护与主动能力执行之间的结果。 */
    public static void run() {
        GhostModuleRegistry.register(new GhostShroudModule());
        var definition = new GhostShroudModule();
        check(definition.getInitialData().intensity() == 30, "初始强度应为30");
        check(definition.corrosion().get(SpiritualPart.SKIN) == 60
                && definition.corrosion().get(SpiritualPart.GLOBAL) == 0, "驾驭侵蚀应为皮肤60");

        var partial = new Host();
        partial.maximum = 100;
        var penetrating = attack(40);
        GhostModuleRuntime.dispatch(partial, penetrating);
        check(!penetrating.isBlocked() && penetrating.intensity() == 40
                && penetrating.remainingIntensity() == 10 && partial.maximum == 70,
                "强度30应抵挡40中的30，支付30代价并留下10");
        var exact = attack(30);
        GhostModuleRuntime.dispatch(partial, exact);
        check(exact.isBlocked() && exact.remainingIntensity() == 0 && partial.maximum == 40,
                "同强度应完整抵挡");
        partial.strength = 5;
        var changed = attack(8);
        GhostModuleRuntime.dispatch(partial, changed);
        check(changed.remainingIntensity() == 3 && partial.maximum == 35, "必须读取实例强度而非写死30");
        partial.strength = 0;
        var powerless = attack(8);
        GhostModuleRuntime.dispatch(partial, powerless);
        check(powerless.remainingIntensity() == 8 && partial.maximum == 35, "零强度不能消耗生命或抵挡");

        var front = new Host();
        var back = new Host();
        front.maximum = 100;
        back.maximum = 100;
        var layered = attack(40);
        GhostModuleRuntime.dispatch(List.of(front, back), layered);
        check(layered.isBlocked() && front.maximum == 70 && back.maximum == 90,
                "后层只抵挡前层留下的10，不重复扣除30");
        var unable = new Host();
        var unpaid = attack(40);
        GhostModuleRuntime.dispatch(unable, unpaid);
        check(unpaid.remainingIntensity() == 40 && unable.maximum == 20, "代价不足不能凭空削弱袭击");

        var first = new Host();
        var second = new Host();
        var event = attack(3);
        GhostModuleRuntime.dispatch(List.of(first, second), event);
        check(event.isBlocked() && first.maximum == 17 && second.maximum == 20,
                "一次袭击只能支付一次代价");
        check(first.paid + second.paid == 1, "后续保护不能重复执行");
        GhostModuleRuntime.dispatch(first, attack(0.7));
        check(first.maximum == 16, "不足一点的强度仍消耗一点生命上限");

        first.maximum = 3;
        var tooStrong = attack(3);
        GhostModuleRuntime.dispatch(first, tooStrong);
        check(!tooStrong.isBlocked() && first.maximum == 3, "不足时必须失败且不扣费");
        var boundary = attack(2);
        GhostModuleRuntime.dispatch(first, boundary);
        check(boundary.isBlocked() && first.maximum == 1, "恰好保留一点应成功");

        var uncovered = new Host();
        uncovered.environment.covered = false;
        var missed = attack(1);
        GhostModuleRuntime.dispatch(uncovered, missed);
        check(!missed.isBlocked() && uncovered.paid == 0, "未覆盖不能抵挡");
        var unrelated = new SupernaturalAttackEvent(UUID.randomUUID(), 1);
        GhostModuleRuntime.dispatch(second, unrelated);
        check(!unrelated.isBlocked(), "不能保护未覆盖的其他实体");

        var locked = new Host();
        locked.deadlocked = true;
        var lockedAttack = attack(1);
        GhostModuleRuntime.dispatch(locked, lockedAttack);
        check(!lockedAttack.isBlocked(), "死机必须停止自动规则");
        var active = new GhostAbilityContext(locked, GhostShroudModule.ID, GhostShroudModule.PROTECT,
                GhostAbilityContext.Source.POSSESSION, lockedAttack);
        check(GhostAbilityExecutor.tryExecuteWithResult(active) && lockedAttack.isBlocked(),
                "主动调用可以绕过死机规则");
        locked.environment.covered = false;
        check(!GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(locked,
                GhostShroudModule.ID, GhostShroudModule.PROTECT, GhostAbilityContext.Source.POSSESSION, attack(1))),
                "主动调用仍不能绕过覆盖媒介");

        var erosion = new Host();
        for (int i = 0; i < 499; i++) GhostModuleRuntime.dispatch(erosion, new ModuleTickEvent());
        check(erosion.drained == 0 && erosion.data.getInt("erosionTicks") == 499, "未满25秒不能扣血");
        erosion.environment.covered = false;
        GhostModuleRuntime.dispatch(erosion, new ModuleTickEvent());
        check(erosion.data.getInt("erosionTicks") == 499, "未覆盖时暂停计时");
        var restored = new Host();
        restored.data = erosion.data.copy();
        GhostModuleRuntime.dispatch(restored, new ModuleTickEvent());
        check(restored.drained == 1 && restored.data.getInt("erosionTicks") == 0, "保存或迁移后继续计时");
        restored.deadlocked = true;
        for (int i = 0; i < 500; i++) GhostModuleRuntime.dispatch(restored, new ModuleTickEvent());
        check(restored.drained == 1, "死机不能继续自动侵蚀");
        System.out.println("Ghost shroud module regression checks passed");
    }
}
