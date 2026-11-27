package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.curse.*;
import com.qidate.qisplan2.ghost.curse.type.ghosttombstone.GhostTombstoneCurseType;
import com.qidate.qisplan2.death.GhostAttackMode;
import com.qidate.qisplan2.death.GhostBodyIdentity;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.GhostPersistentSuppression;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppression;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionState;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.*;
import com.qidate.qisplan2.ghost.module.component.*;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostAttackContext;
import com.qidate.qisplan2.ghost.module.implementation.divinationslip.*;
import com.qidate.qisplan2.ghost.module.implementation.eye.GhostEyeModule;
import com.qidate.qisplan2.ghost.module.implementation.mist.GhostMistModule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** 组件声明、事件订阅、媒介、持续诅咒存档和压制声明的离线验证。 */
public final class CurseComponentTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private record Unrelated() implements GhostEvent {}
    public static void run(){
        check(!new GhostDivinationCurseType().persistsAfterDeath(),"life slip must expire on death");
        check(!new GhostRevivalCurseType().persistsAfterDeath(),"revival slip must expire on death");
        check(new GhostTombstoneCurseType().persistsAfterDeath(),"tombstone must persist after death");
        var ability=ResourceLocation.parse("qisplan2_test:component");
        int[] count={0};boolean[] medium={false};
        var program=new GhostComponentProgram<Integer>(List.of(new GhostComponentProgram.Ability<>(ability,c->medium[0],c->count[0]+=c)),
                List.of(new GhostComponentProgram.Rule<>(ModuleTickEvent.class,c->true,ability)));
        program.dispatch(new ModuleTickEvent(),2);check(count[0]==0,"rule invocation must respect medium");
        check(!program.execute(ability,2),"direct invocation must respect same medium");
        medium[0]=true;program.dispatch(new Unrelated(),10);check(count[0]==0,"unsubscribed events must not execute");
        program.dispatch(new ModuleTickEvent(),2);check(count[0]==2,"subscribed rule executes ability");
        var source=new GhostDivinationCurseSource();var target=UUID.randomUUID();
        var data=new CompoundTag();data.putInt("Strength",73);data.putInt("RemainingTicks",321);
        var type=new GhostRevivalCurseType();var curse=type.create(target,source,data);var restored=type.load(curse.save());
        check(restored.getId().equals(curse.getId())&&restored.getTarget().equals(target)&&restored.getStrength()==73&&restored.getRemainingTicks()==321,"curse identity strength and duration survive save/load");
        check(type.revivalMultiplier()==2&&type.suppression().equals(GhostSuppressionAura.NONE),"ghost slip doubles revival without emitting suppression");
        restored.tickDormant();check(restored.getRemainingTicks()==320,"dormancy does not freeze duration");
        CurseManager.restore(restored);check(CurseManager.getByTarget(target).size()==1,"restore builds target index");
        CurseManager.clear();check(CurseManager.getByTarget(target).isEmpty(),"world reset clears target index");
        var life=new GhostDivinationCurseType();
        for(double incoming:List.of(20D,80D)){
            var damage=new SupernaturalAttackEvent(target,incoming);
            var response=new GhostAttackContext(damage,null,()->{},()->incoming);
            var event=new SupernaturalSuppressionEvent(target,incoming);
            life.program().dispatch(event,new CurseAbilityContext(UUID.randomUUID(),target,50,event,response,()->{},null));
            check(damage.isBlocked()==(incoming<50),"life curse must confront suppression using own strength");
            var raw=new GhostSuppressionState(GhostSuppression.Level.COMPLETE,GhostSuppression.Level.NONE,incoming);
            var blockers=damage.isBlocked()?List.of(new CurseManager.SuppressionBlocker(UUID.randomUUID(),life.id(),50)):List.<CurseManager.SuppressionBlocker>of();
            var report=GhostPersistentSuppression.protectionReport(raw,new CurseManager.SuppressionResult(damage,blockers));
            check(report.raw().external()==GhostSuppression.Level.COMPLETE,"诊断保留原始完全压制");
            check(report.effective().canAct()==damage.isBlocked()&&report.effective().canRunRules()==damage.isBlocked()
                    &&report.effective().canUseAbility(true)==damage.isBlocked(),"最终许可必须反映真实生签抵挡结果");
            check(report.blockers().size()==(damage.isBlocked()?1:0),"没有抵挡不能伪造保护来源");
        }
        // 身份判定不读取模块表：承载者仍可被袭击，只有本体声明为鬼才走压制。
        GhostModuleHost possessor = () -> List.of(new GhostModuleData.Entry(ResourceLocation.parse("qisplan2_test:carried"), 10));
        SupernaturalEntity ghost = new SupernaturalEntity() {
            public double getSupernaturalStrength() { return 0; }
            public boolean isSupernaturallyStunned() { return true; }
        };
        GhostBodyIdentity human = () -> false;
        check(!GhostBodyIdentity.isGhost(possessor) && !GhostBodyIdentity.isGhost(human), "承载模块不能赋予厉鬼本体身份");
        check(GhostAttackMode.AUTO.resolve(GhostBodyIdentity.isGhost(possessor)) == GhostAttackMode.ATTACK,
                "驭鬼者的自动意图必须为袭击");
        check(GhostAttackMode.AUTO.resolve(GhostBodyIdentity.isGhost(ghost)) == GhostAttackMode.SUPPRESSION,
                "真正厉鬼本体的自动意图必须为压制");
        check(GhostAttackMode.AUTO.resolve(true)==GhostAttackMode.SUPPRESSION
                && GhostAttackMode.AUTO.resolve(false)==GhostAttackMode.ATTACK
                && GhostAttackMode.ATTACK.resolve(true)==GhostAttackMode.ATTACK,"自动路由不能改变显式袭击语义");
        for(var eventType:List.of(GhostAttackMode.ATTACK,GhostAttackMode.SUPPRESSION)){
            boolean[] deadlock={false},removed={false};
            var damage=new SupernaturalAttackEvent(target,50);
            var response=new GhostAttackContext(damage,GhostDivinationSlipModule.ID,()->deadlock[0]=true,()->50);
            GhostEvent event=eventType==GhostAttackMode.ATTACK?damage:new SupernaturalSuppressionEvent(target,50);
            life.program().dispatch(event,new CurseAbilityContext(UUID.randomUUID(),target,50,event,response,()->removed[0]=true,null));
            check(deadlock[0]&&removed[0]&&damage.isDeadlocked(),"生签与同源死签在袭击及压制路径均须裁决死机");
        }
        var eye=new GhostEyeModule().domainDefinition().orElseThrow();
        check(eye.suppression().offer(4,100)==null&&eye.suppression().offer(5,100).strength()==100,"eye aura requires grade five");
        check(new GhostMistModule().domainDefinition().orElseThrow().suppression().offer(1,37).strength()==37,"mist aura uses actual effective strength");
        check(eye.withLayering(false).suppression().equals(eye.suppression()),"copy builders preserve component declaration");
        // 旧测试使用同名假模块验证裁决，独立测试模块承载真实声明，避免覆盖其他用例。
        var owner=ResourceLocation.parse("qisplan2_test:curse_owner");
        GhostModuleRegistry.register(new GhostModule(){
            public ResourceLocation getId(){return owner;}
            public List<CurseType> curses(){return new GhostDivinationSlipModule().curses();}
        });
        check(owner.equals(CurseRegistry.owner(GhostRevivalCurseType.ID)),"curse registration must derive from complete module");
        check(CurseRegistry.get(GhostDivinationCurseType.ID)!=null,"legacy life curse ID stays readable");
        System.out.println("Curse component subscription, media, persistence, ownership, suppression and aura checks passed");
    }
}
