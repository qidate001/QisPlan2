package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.suppression.GhostSuppressionState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.*;

/** 验证实际派发只调用订阅者，而不是把全量分发藏在类型过滤方法后。 */
public final class EventSubscriptionTest {
    private interface ParentFact extends GhostEvent {}
    private record Fact() implements ParentFact {}
    private record OtherFact() implements GhostEvent {}
    private static int declarations,matched,triggered,ability;
    private EventSubscriptionTest() {}
    private static void check(boolean result,String message){if(!result)throw new AssertionError(message);}
    /** 父接口、重复订阅、空订阅、晚注册和同批次移除均走真实运行时。 */
    public static void run() {
        List<GhostModuleData.Entry> entries=new ArrayList<>();
        boolean[] removeLate={false};
        var first=ResourceLocation.parse("qisplan2:test_subscribed");
        var irrelevant=ResourceLocation.parse("qisplan2:test_unrelated");
        GhostModuleRegistry.register(new GhostModule() {
            public ResourceLocation getId(){return first;}
            public Set<Class<? extends GhostEvent>> abilityEventTypes(){return Set.of(ParentFact.class,Fact.class);}
            public void onAbilityEvent(GhostModuleContext c,GhostEvent event){
                ability++;
                if(removeLate[0])entries.removeIf(e->e.id().equals(ResourceLocation.parse("qisplan2:test_subscription_late")));
            }
            public List<GhostRule> getRules(){
                declarations++;
                return List.of(new GhostRule() {
                    public ResourceLocation getId(){return first;}
                    public Set<Class<? extends GhostEvent>> eventTypes(){return Set.of(ParentFact.class,Fact.class);}
                    public boolean matches(GhostEvent event){matched++;return true;}
                    public boolean canTrigger(GhostRuleContext c,GhostEvent event){return true;}
                    public void onTrigger(GhostRuleContext c,GhostEvent event){triggered++;}
                });
            }
        });
        GhostModuleRegistry.register(new GhostModule() {
            public ResourceLocation getId(){return irrelevant;}
            public void onAbilityEvent(GhostModuleContext c,GhostEvent e){throw new AssertionError("未订阅能力不得收到广播");}
            public void tickAbilities(GhostModuleContext c){throw new AssertionError("未声明Tick不得调用维护");}
            public List<GhostRule> getRules(){
                declarations++;
                return List.of(new GhostRule() {
                    public ResourceLocation getId(){return irrelevant;}
                    public Set<Class<? extends GhostEvent>> eventTypes(){return Set.of(OtherFact.class);}
                    public boolean matches(GhostEvent e){throw new AssertionError("无关规则的过滤器也不能被调用");}
                    public boolean canTrigger(GhostRuleContext c,GhostEvent e){return true;}
                    public void onTrigger(GhostRuleContext c,GhostEvent e){throw new AssertionError("不应派发");}
                });
            }
        });
        entries.add(entry(first));entries.add(entry(irrelevant));
        GhostModuleHost host=(GhostModuleHost)Proxy.newProxyInstance(EventSubscriptionTest.class.getClassLoader(),new Class<?>[]{GhostModuleHost.class},(p,m,a)->switch(m.getName()) {
            case "getModules"->List.copyOf(entries);
            case "carriesModule"->entries.stream().anyMatch(e->e.id().equals(a[0]));
            case "suppression"->GhostSuppressionState.NONE;
            case "controlsIntrinsicModules"->true;
            default->{if(m.isDefault())yield InvocationHandler.invokeDefault(p,m,a);throw new AssertionError(m.getName());}
        });
        for(int i=0;i<10;i++)GhostModuleRuntime.dispatch(host,new Fact());
        check(declarations==2,"规则定义只在注册时读取一次，热派发不调用 getRules");
        check(matched==10&&triggered==10&&ability==10,"父接口与具体类型同时命中只执行一次");
        check(GhostEventSubscriptions.subscribers(ModuleTickEvent.class).stream().noneMatch(d->d.module().getId().equals(irrelevant)),"没有隐式持续能力Tick订阅");
        var cached=GhostEventSubscriptions.subscribers(Fact.class);
        check(cached==GhostEventSubscriptions.subscribers(Fact.class),"重复类型查询复用不可变计划");
        var late=ResourceLocation.parse("qisplan2:test_subscription_late");int[] lateCalls={0};
        GhostModuleRegistry.register(new GhostModule() {
            public ResourceLocation getId(){return late;}
            public Set<Class<? extends GhostEvent>> abilityEventTypes(){return Set.of(ParentFact.class);}
            public void onAbilityEvent(GhostModuleContext c,GhostEvent e){lateCalls[0]++;}
        });
        entries.add(entry(late));GhostModuleRuntime.dispatch(host,new Fact());
        check(lateCalls[0]==1,"注册后清理旧派发计划，新订阅可见");
        entries.removeIf(e->e.id().equals(first));int before=triggered;
        GhostModuleRuntime.dispatch(host,new Fact());check(triggered==before&&lateCalls[0]==2,"模块移除立即停止派发，不依赖失效的宿主缓存");
        entries.add(0,entry(first));removeLate[0]=true;
        GhostModuleRuntime.dispatch(host,new Fact());
        check(lateCalls[0]==2,"前一订阅回调移除后续模块时，旧快照也不能继续派发");
        System.out.println("Indexed event subscription, inheritance, deduplication, cache and removal checks passed");
    }
    /** 创建独立的测试实例状态，不共享可变 customData。 */
    private static GhostModuleData.Entry entry(ResourceLocation id){return new GhostModuleData.Entry(id,10,0,false,false,new CompoundTag());}
}
