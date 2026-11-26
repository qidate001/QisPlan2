package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.curse.*;
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
