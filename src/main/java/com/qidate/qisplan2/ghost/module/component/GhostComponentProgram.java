package com.qidate.qisplan2.ghost.module.component;

import java.util.*;
import java.util.function.*;
import net.minecraft.resources.ResourceLocation;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;

/** 模块所属持续能力的规则、能力和媒介声明。程序只接收语义上下文，不接收宿主身份。 */
public final class GhostComponentProgram<C> {
    /** 能力媒介是执行前的硬性检查；规则触发与直接调用走同一个检查入口。 */
    public record Ability<C>(ResourceLocation id, Predicate<C> medium, Consumer<C> effect) {
        public Ability { Objects.requireNonNull(id); Objects.requireNonNull(medium); Objects.requireNonNull(effect); }
    }
    /** 规则订阅一种语义事件，条件成立后调用声明的能力。 */
    public record Rule<C>(Class<? extends GhostEvent> event, Predicate<C> condition, ResourceLocation ability) {
        public Rule { Objects.requireNonNull(event); Objects.requireNonNull(condition); Objects.requireNonNull(ability); }
    }
    private final Map<ResourceLocation, Ability<C>> abilities;
    private final Map<Class<?>, List<Rule<C>>> rules;
    private final Map<Class<?>, List<Rule<C>>> routes = new HashMap<>();
    /** 声明时建立索引并验证引用，事件发生时不广播给所有规则。 */
    public GhostComponentProgram(List<Ability<C>> abilities, List<Rule<C>> rules) {
        var indexed = new LinkedHashMap<ResourceLocation, Ability<C>>();
        for(var ability:abilities) if(indexed.putIfAbsent(ability.id(),ability)!=null) throw new IllegalArgumentException("Duplicate component ability");
        this.abilities=Map.copyOf(indexed);
        var subscriptions=new HashMap<Class<?>,List<Rule<C>>>();
        for(var rule:rules) {
            if(!indexed.containsKey(rule.ability()))throw new IllegalArgumentException("Undeclared component ability");
            subscriptions.computeIfAbsent(rule.event(),k->new ArrayList<>()).add(rule);
        }
        subscriptions.replaceAll((k,v)->List.copyOf(v));this.rules=Map.copyOf(subscriptions);
    }
    /** 空组件不会注册任何监听。 */
    public static <C> GhostComponentProgram<C> empty(){return new GhostComponentProgram<>(List.of(),List.of());}
    /** 供诊断、工具与模块声明查询；不可在运行时改写定义。 */
    public Collection<Ability<C>> abilities(){return abilities.values();}
    /** 直接调用也必须满足该能力媒介。 */
    public boolean execute(ResourceLocation id,C context){var ability=abilities.get(id);if(ability==null||!ability.medium().test(context))return false;ability.effect().accept(context);return true;}
    /** 按事件类型索引查询；继承路由只构建一次，避免每次遍历所有规则。 */
    public void dispatch(GhostEvent event,C context){
        var listeners=routes.computeIfAbsent(event.getClass(),type->{
            var result=new ArrayList<Rule<C>>();var seen=new HashSet<Class<?>>();var pending=new ArrayDeque<Class<?>>();pending.add(type);
            while(!pending.isEmpty()){var next=pending.remove();if(!seen.add(next))continue;result.addAll(rules.getOrDefault(next,List.of()));if(next.getSuperclass()!=null)pending.add(next.getSuperclass());pending.addAll(List.of(next.getInterfaces()));}
            return List.copyOf(result);
        });
        for(var rule:listeners)if(rule.condition().test(context))execute(rule.ability(),context);
    }
}
