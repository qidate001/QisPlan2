package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleContainer;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppression;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionState;
import java.lang.reflect.Proxy;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** 使用纯接口宿主验证跨宿主状态迁移、连续压制、自然苏醒及伪规则，不启动游戏。 */
public final class SuppressionSleepTest {
    private static final ResourceLocation ID = ResourceLocation.parse("qisplan2_test:sleep");
    private static final ResourceLocation RULE = ResourceLocation.parse("qisplan2_test:sleep/rule");
    private record Violation() implements GhostEvent {}
    private static int effects;
    /** 断言失败使构建失败。 */
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    /** 可替换为任意载体的测试存储，时间与压制均由端口提供。 */
    private static final class Host implements GhostModuleDataModifier, GhostModuleCustomDataModifier {
        final GhostModuleContainer data = new GhostModuleContainer();
        long time;
        double pressure;
        Host() { data.loadEntries(List.of(new GhostModuleData.Entry(ID, 100))); }
        public List<GhostModuleData.Entry> getModules() { return data.getModules(); }
        public boolean addModule(ResourceLocation id, double strength) { return data.addModuleIfAbsent(id, strength); }
        public boolean removeModule(ResourceLocation id) { return data.removeModule(id); }
        public boolean setIntensity(ResourceLocation id, double value) { return data.setIntensity(id, value); }
        public boolean setRevival(ResourceLocation id, double value) { return data.setRevival(id, value); }
        public boolean setSleeping(ResourceLocation id, boolean value) { return data.setSleeping(id, value); }
        public boolean setDeadlocked(ResourceLocation id, boolean value) { return data.setDeadlocked(id, value); }
        public CompoundTag getModuleCustomData(ResourceLocation id) { return data.getCustomData(id); }
        public boolean setModuleCustomData(ResourceLocation id, CompoundTag value) { return data.setCustomData(id, value); }
        public GhostWorldAccess moduleWorld(ResourceLocation id) {
            return (GhostWorldAccess)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{GhostWorldAccess.class},
                    (proxy, method, args) -> switch(method.getName()) {
                        case "gameTime" -> time;
                        case "present" -> true;
                        default -> throw new AssertionError("状态处理不应调用世界效果: " + method.getName());
                    });
        }
        public GhostSuppressionState suppression(ResourceLocation id) {
            var declared = pressure == Double.POSITIVE_INFINITY ? GhostSuppression.absolute()
                    : new GhostSuppression(GhostSuppression.Kind.NORMAL, pressure);
            return new GhostSuppressionState(declared.level(data.getModule(id).intensity()), GhostSuppression.Level.NONE, pressure)
                    .merge(GhostModuleCombatState.attackSuppression(this, id));
        }
        GhostModuleData.Entry entry() { return data.getModule(ID); }
        void advance(int ticks) { for (int i=0; i<ticks; i++) { time++; GhostModuleCombatState.tick(this); } }
    }
    /** 测试正式状态逻辑，覆盖时间阈值、断续、上限、迁移及短时袭击到期。 */
    public static void run() {
        GhostModuleRegistry.register(new GhostModule() {
            public ResourceLocation getId() { return ID; }
            public List<GhostRule> getRules() { return List.of(new GhostRule() {
                public ResourceLocation getId() { return RULE; }
                public boolean supports(GhostEvent event) { return event instanceof Violation || event instanceof ModuleTickEvent; }
                public boolean canTrigger(GhostRuleContext c, GhostEvent event) { return true; }
                public void onTrigger(GhostRuleContext c, GhostEvent event) { effects++; }
            }); }
        });
        var base = new Host(); base.pressure=150;
        base.advance(5999); check(!base.entry().sleeping(), "基准完整压制不足五分钟不入睡");
        base.advance(1); check(base.entry().sleeping(), "恰好1.5倍时五分钟入睡");
        base.advance(12000); check(GhostModuleCombatState.sleepTicks(base.entry()) <= 12001, "基础沉睡最多十分钟");
        int stable = GhostModuleCombatState.sleepTicks(base.entry()); var frozen = base.getModuleCustomData(ID); base.advance(100);
        check(frozen.equals(base.getModuleCustomData(ID)), "稳定上限不重复写入或同步状态");
        check(GhostModuleCombatState.sleepTicks(base.entry()) == stable, "继续压制不能突破上限");
        base.pressure=0; base.advance(stable); check(!base.entry().sleeping(), "无压制时自然苏醒");
        var strong = new Host(); strong.pressure=300; strong.advance(1499);
        check(!strong.entry().sleeping(), "三倍强度需要七十五秒"); strong.advance(1);
        check(strong.entry().sleeping(), "平方倍率加速入睡");
        check(GhostModuleCombatState.sleepCap(300,100)==48000, "三倍强度沉睡上限四十分钟");
        var absolute = new Host(); absolute.pressure=Double.POSITIVE_INFINITY;
        absolute.advance(299); check(!absolute.entry().sleeping(), "无解压制不足十五秒不入睡");
        absolute.advance(1); check(absolute.entry().sleeping(), "无解压制十五秒入睡");
        check(GhostModuleCombatState.sleepCap(Double.POSITIVE_INFINITY,100)==1728000, "无解上限二十四小时");
        var interrupted = new Host(); interrupted.pressure=300; interrupted.advance(1000);
        interrupted.pressure=0; interrupted.advance(1); interrupted.pressure=300; interrupted.advance(500);
        check(!interrupted.entry().sleeping(), "中断完全压制必须重计连续入睡进度");
        var attack = new Host(); GhostModuleCombatState.attacked(attack,300); attack.advance(10);
        GhostModuleCombatState.attacked(attack,300); attack.time=29;
        check(!attack.suppression(ID).canUseAbility(false), "第二次袭击刷新至命中后一秒");
        attack.time=30; check(attack.suppression(ID).canAct(), "到期立即解除，不累加第二段一秒");
        check(new GhostSuppression(GhostSuppression.Kind.NORMAL,0).level(0)==GhostSuppression.Level.NONE, "零对零不能形成完全压制");
        var weak = new Host(); GhostModuleCombatState.attacked(weak,50);
        check(weak.suppression(ID).canAct(), "弱袭击不能绕过强度阈值强制停滞");
        GhostModuleCombatState.sleep(weak,weak.entry(),1200);
        int before=effects; GhostModuleRuntime.dispatch(weak,new Violation());
        check(effects==before && GhostModuleCombatState.sleepTicks(weak.entry())==200, "触犯只加速苏醒，不执行规则效果");
        GhostModuleRuntime.dispatch(weak,new Violation());
        check(GhostModuleCombatState.sleepTicks(weak.entry())==200, "同秒重复事件不能无限唤醒");
        weak.time+=20; GhostModuleRuntime.dispatch(weak,new Violation());
        check(!weak.entry().sleeping() && effects==before, "唤醒的这次事件也不能反击");
        GhostModuleRuntime.dispatch(weak,new Violation()); check(effects==before+1, "苏醒后的新事件恢复规则");
        GhostModuleCombatState.sleep(weak,weak.entry(),1200);
        var moved=new Host(); moved.data.loadEntries(weak.getModules()); moved.advance(1);
        check(GhostModuleCombatState.sleepTicks(moved.entry())==1199, "转移完整模块后沉睡进度保留并自然恢复");
        moved.setDeadlocked(ID,true); moved.advance(1199);
        check(!moved.entry().sleeping() && moved.entry().deadlocked(), "自然苏醒不能解除裁决死机");
        var legacy=new Host(); var custom=new CompoundTag(); var old=new CompoundTag();
        old.putInt("stunTicks",200); old.putBoolean("permanentStun",true); custom.put(GhostModuleCombatState.KEY,old);
        legacy.setModuleCustomData(ID,custom); legacy.advance(1);
        check(legacy.entry().sleeping() && legacy.entry().deadlocked(), "旧有限和永久停滞分别迁移沉睡和裁决死机");
        check(!legacy.getModuleCustomData(ID).getCompound(GhostModuleCombatState.KEY).contains("stunTicks"), "迁移后移除旧计时字段");
        System.out.println("Suppression sleep, awakening, migration and one-second attack checks passed");
    }
}
