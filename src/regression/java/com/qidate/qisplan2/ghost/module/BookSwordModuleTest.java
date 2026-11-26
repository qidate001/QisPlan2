package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.environment.GhostDeathCurseAccess;
import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.implementation.GhostBookModule;
import com.qidate.qisplan2.ghost.module.implementation.DeathCurseSwordModule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.List;

/** 不提供任何玩家或物品载体身份，执行真实模块与执行器来验证迁移边界。 */
public final class BookSwordModuleTest {
    private static final class Subject implements GhostDeathCurseAccess.Subject {
        int count, strikes;
        boolean reflected;
        double strength;
        /** 模拟共享目标层数。 */
        public int stacks() { return count; }
        /** 写回实际层数。 */
        public void stacks(int value) { count=value; }
        /** 记录模块请求的袭击；必须先清零再提交效果，避免重复阈值触发。 */
        public void strike(GhostAbilityContext context,double intensity,boolean backlash) {
            check(count==0,"触发袭击前必须清空十层计数"); strikes++; strength=intensity; reflected=backlash;
        }
    }
    private static final class Host implements GhostModuleHost {
        final ResourceLocation id;
        final Subject victim=new Subject(), reflected=new Subject();
        boolean deadlocked, sleeping, allowed=true;
        double random=1;
        int exhausted, screens;
        /** 用同一个抽象宿主验证两种模块。 */
        Host(ResourceLocation id) { this.id=id; }
        /** 强度使用非默认值，验证效果取自模块实例而非旧常量。 */
        public List<GhostModuleData.Entry> getModules() {
            return List.of(new GhostModuleData.Entry(id,17,0,sleeping,deadlocked,new CompoundTag()));
        }
        /** 只实现模块允许访问的端口，其余访问立即失败。 */
        public GhostWorldAccess moduleWorld(ResourceLocation ignored) {
            var curse=new GhostDeathCurseAccess() {
                /** 在测试中模拟媒介是否给出可作用目标。 */
                public Subject target(LivingEntity target) { return allowed ? victim : null; }
                /** 反噬对象也是不透明端口，不暴露实体。 */
                public Subject backlash() { return reflected; }
                /** 固定抽样使概率边界可重复验证。 */
                public double random() { return random; }
                /** 记录来源消耗次数。 */
                public void exhaust() { exhausted++; }
            };
            return (GhostWorldAccess)Proxy.newProxyInstance(GhostWorldAccess.class.getClassLoader(),new Class<?>[]{GhostWorldAccess.class},
                    (proxy,method,args)->switch(method.getName()) {
                        case "deathCurses" -> curse;
                        case "aimedTarget" -> null;
                        case "canOpenScreen" -> allowed;
                        case "openScreen" -> { check(allowed,"禁止绕过读者媒介"); screens++; yield true; }
                        default -> {
                            if (method.isDefault()) yield InvocationHandler.invokeDefault(proxy,method,args);
                            throw new AssertionError("Unexpected world access: "+method.getName());
                        }
                    });
        }
    }
    /** 失败时给出具体行为说明。 */
    private static void check(boolean value,String message) { if (!value) throw new AssertionError(message); }
    /** 始终使用统一执行器，测试不会绕过媒介或模块状态。 */
    private static boolean invoke(Host host,ResourceLocation ability,GhostAbilityContext.Source source) {
        return GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host,host.id,ability,source,null));
    }
    /** 验证阈值、反噬、消耗、死机、无目标和公共能力目录。 */
    public static void run() {
        if (!GhostModuleRegistry.contains(GhostBookModule.ID)) GhostModuleRegistry.register(new GhostBookModule());
        if (!GhostModuleRegistry.contains(DeathCurseSwordModule.ID)) GhostModuleRegistry.register(new DeathCurseSwordModule());
        var sword=new Host(DeathCurseSwordModule.ID);
        for(int i=0;i<9;i++) check(invoke(sword,DeathCurseSwordModule.CURSE,GhostAbilityContext.Source.POSSESSION),"前九次施咒应成功叠层");
        check(sword.victim.count==9 && sword.victim.strikes==0 && sword.exhausted==0,"未达十层不得提前袭击或破碎");
        invoke(sword,DeathCurseSwordModule.CURSE,GhostAbilityContext.Source.POSSESSION);
        check(sword.victim.count==0 && sword.victim.strikes==1 && sword.victim.strength==17 && sword.exhausted==1,
                "第十层使用实例强度袭击、清零并消耗一次");
        var both=new Host(DeathCurseSwordModule.ID); both.victim.count=9; both.reflected.count=9; both.random=.499;
        invoke(both,DeathCurseSwordModule.CURSE,GhostAbilityContext.Source.POSSESSION);
        check(both.victim.strikes==1 && both.reflected.strikes==1 && both.reflected.reflected && both.exhausted==1,
                "双方同时满层都结算，来源只消耗一次");
        var boundary=new Host(DeathCurseSwordModule.ID); boundary.random=.5;
        invoke(boundary,DeathCurseSwordModule.CURSE,GhostAbilityContext.Source.POSSESSION);
        check(boundary.reflected.count==0,"抽样值 0.5 不属于小于 50% 的反噬区间");
        boundary.deadlocked=true;
        check(!invoke(boundary,DeathCurseSwordModule.CURSE,GhostAbilityContext.Source.RULE),"死机后自动规则能力不得执行");
        check(invoke(boundary,DeathCurseSwordModule.CURSE,GhostAbilityContext.Source.POSSESSION),"死机后主动施咒仍可使用");
        boundary.allowed=false; boundary.random=0;
        check(!invoke(boundary,DeathCurseSwordModule.CURSE,GhostAbilityContext.Source.POSSESSION)
                && boundary.reflected.count==0,"媒介失败不能叠层或额外反噬");
        check(GhostEventSubscriptions.rules(DeathCurseSwordModule.ID,ModuleTickEvent.class).isEmpty(),"持有采样不能代替近战命中叠层");
        var book=new Host(GhostBookModule.ID); book.deadlocked=true; book.sleeping=true;
        check(invoke(book,GhostBookModule.READ,GhostAbilityContext.Source.POSSESSION) && book.screens==1,"死机或沉睡不禁止手动阅读能力");
        book.allowed=false;
        check(!invoke(book,GhostBookModule.READ,GhostAbilityContext.Source.POSSESSION) && book.screens==1,"媒介失败不能发送界面通知");
        check(GhostAbilityCatalog.all(book).size()==1 && GhostAbilityCatalog.all(sword).size()==1,
                "鬼书只公开阅读，剑只公开施咒，不能直接选择绕过十层的袭击");
        System.out.println("Ghost book and death curse sword module regression checks passed");
    }
}
