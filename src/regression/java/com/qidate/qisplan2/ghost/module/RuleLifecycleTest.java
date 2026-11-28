package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** 验证规则多入口、实例状态隔离和无需实体的强度调度。 */
public final class RuleLifecycleTest {
    private static final ResourceLocation ID=ResourceLocation.parse("qisplan2_test:state_rule");
    private record Begin() implements GhostEvent {}
    private record Finish() implements GhostEvent {}
    private static final class Host implements GhostModuleHost,GhostModuleCustomDataModifier {
        private GhostModuleData.Entry entry=new GhostModuleData.Entry(ID,10);
        public List<GhostModuleData.Entry> getModules(){return List.of(entry);}
        public CompoundTag getModuleCustomData(ResourceLocation id){return entry.customData();}
        public boolean setModuleCustomData(ResourceLocation id,CompoundTag data){entry=entry.withCustomData(data);return true;}
    }
    private static final class Task extends GhostRuleTask {
        int probes,starts,stops,ticks;
        Task(Flag flag){setFlags(EnumSet.of(flag));}
        public boolean canUse(){probes++;return true;}
        public void start(){starts++;}
        public void stop(){stops++;}
        public void tick(){ticks++;}
    }
    /** 断言失败时提供具体行为说明。 */
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    /** 多个语义入口共享实例阶段，强度调度不需要 Minecraft 实体。 */
    public static void run(){
        var rule=new GhostStateMachineRule(){
            public ResourceLocation getId(){return ID;}
            public Set<Class<? extends GhostEvent>> eventTypes(){return Set.of(Begin.class,Finish.class);}
            protected void onEvent(GhostRuleContext context,String phase,GhostEvent event){
                if(event instanceof Begin && phase.equals("idle"))transition(context,"waiting",event);
                if(event instanceof Finish && phase.equals("waiting"))transition(context,"done",event);
            }
        };
        GhostModuleRegistry.register(new GhostModule(){
            public ResourceLocation getId(){return ID;}
            public List<GhostRule> getRules(){return List.of(rule);}
        });
        Host first=new Host(),second=new Host();
        GhostModuleRuntime.dispatch(first,new Begin());
        check(rule.state(new GhostRuleContext(first,ID,ID)).equals("waiting"),"begin enters waiting");
        GhostModuleRuntime.dispatch(second,new Finish());
        check(rule.state(new GhostRuleContext(second,ID,ID)).equals("idle"),"instances must not share state");
        var saved=first.entry.customData();
        Host restored=new Host();restored.entry=restored.entry.withCustomData(saved);
        GhostModuleRuntime.dispatch(restored,new Finish());
        check(rule.state(new GhostRuleContext(restored,ID,ID)).equals("done"),"different entry resumes saved state");
        first.entry=new GhostModuleData.Entry(ID,10,0,false,true,saved);
        GhostModuleRuntime.dispatch(first,new Finish());
        check(rule.state(new GhostRuleContext(first,ID,ID)).equals("waiting"),"deadlock blocks state advancement");
        long[] time={0};boolean[] available={true};double[] power={10,30};
        List<GhostRuleExecution> peers=new ArrayList<>();
        Task weak=new Task(GhostRuleTask.Flag.MOVE),strong=new Task(GhostRuleTask.Flag.MOVE),look=new Task(GhostRuleTask.Flag.LOOK);
        var a=new GhostRuleExecution(ID,0,weak,()->available[0],()->power[0],()->time[0],()->peers);
        var b=new GhostRuleExecution(ResourceLocation.parse("qisplan2_test:strong"),0,strong,()->true,()->power[1],()->time[0],()->peers);
        var c=new GhostRuleExecution(ResourceLocation.parse("qisplan2_test:look"),0,look,()->true,()->1,()->time[0],()->peers);
        peers.addAll(List.of(a,b,c));
        check(!a.canUse()&&b.canUse()&&c.canUse(),"dominant movement must not block independent look");
        b.canUse();check(strong.probes==1,"demand probes must be cached within tick");
        power[0]=100;time[0]++;
        check(a.canUse()&&!b.canUse(),"strength changes take effect without reinstallation");
        a.start();a.start();a.tick();check(weak.starts==1&&weak.ticks==1,"start is idempotent");
        available[0]=false;a.tick();a.stop();check(weak.stops==1&&weak.ticks==1,"invalid state stops once without effect");
        available[0]=true;power[0]=power[1]=10;time[0]+=10;
        a.start();time[0]+=100;check(!a.canContinueToUse(),"equal strength yields after time slice");
        a.stop();check(!a.canUse(),"yield cooldown prevents immediate reacquisition");
        System.out.println("Rule state isolation, multiple entries, persistence, deadlock and task arbitration passed");
    }
}
