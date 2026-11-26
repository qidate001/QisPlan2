package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** 注册期构建事件反向索引；派发只查询事件类型及其父接口，不扫描模块或规则定义。 */
public final class GhostEventSubscriptions {
    private static final Map<Class<?>,List<Listener>> BY_TYPE=new HashMap<>();
    private static final Map<Class<?>,List<Delivery>> RESOLVED=new HashMap<>();
    private static final Map<ResourceLocation,Map<ResourceLocation,GhostRule>> RULES=new HashMap<>();
    private static int nextOrder;
    private GhostEventSubscriptions() {}

    /** 单个模块在本次事件中的准确接收集合，规则保持原声明顺序。 */
    public record Delivery(GhostModule module,List<GhostRule> rules,boolean abilityEvent,boolean abilityTick) {
        public Delivery { rules=List.copyOf(rules); }
    }
    /** 注册期条目；ruleIndex 保证多父接口命中时只执行一次并保持规则顺序。 */
    private record Listener(GhostModule module,int moduleOrder,GhostRule rule,int ruleIndex,boolean ability,boolean tick) {}
    /** 只在首次遇到具体事件类时合并父接口订阅。 */
    private static final class Builder {
        final GhostModule module;
        final int order;
        final SortedMap<Integer,GhostRule> rules=new TreeMap<>();
        boolean ability,tick;
        Builder(Listener listener){module=listener.module();order=listener.moduleOrder();}
        void add(Listener listener){
            if(listener.rule()!=null)rules.put(listener.ruleIndex(),listener.rule());
            ability|=listener.ability();tick|=listener.tick();
        }
        Delivery build(){return new Delivery(module,new ArrayList<>(rules.values()),ability,tick);}
    }
    /** 先完整验证声明，再原子加入索引；注册失败不会留下部分订阅。 */
    static synchronized void register(GhostModule module) {
        Map<Class<?>,List<Listener>> pending=new HashMap<>();
        Set<ResourceLocation> ids=new HashSet<>();
        List<GhostRule> rules=List.copyOf(module.getRules());
        for(int i=0;i<rules.size();i++) {
            GhostRule rule=rules.get(i);
            if(!ids.add(Objects.requireNonNull(rule.getId())))throw new IllegalArgumentException("Duplicate rule: "+rule.getId());
            Set<Class<? extends GhostEvent>> types=Set.copyOf(rule.eventTypes());
            if(types.isEmpty())throw new IllegalArgumentException("Rule must declare event subscriptions: "+rule.getId());
            for(Class<?> type:types)add(pending,type,new Listener(module,nextOrder,rule,i,false,false));
        }
        for(Class<?> type:Set.copyOf(module.abilityEventTypes()))
            add(pending,type,new Listener(module,nextOrder,null,-1,true,false));
        if(module.ticksAbilities())add(pending,ModuleTickEvent.class,new Listener(module,nextOrder,null,-1,false,true));
        pending.forEach((type,list)->BY_TYPE.computeIfAbsent(type,ignored->new ArrayList<>()).addAll(list));
        Map<ResourceLocation,GhostRule> byId=new HashMap<>();
        rules.forEach(rule->byId.put(rule.getId(),rule));RULES.put(module.getId(),Map.copyOf(byId));
        nextOrder++;RESOLVED.clear();
    }
    /** 拒绝伪造类型，防止不相关的 Java 类型进入事件总线。 */
    private static void add(Map<Class<?>,List<Listener>> map,Class<?> type,Listener listener) {
        if(!GhostEvent.class.isAssignableFrom(type))throw new IllegalArgumentException("Not a ghost event: "+type);
        map.computeIfAbsent(type,ignored->new ArrayList<>()).add(listener);
    }
    /** 热路径为具体事件类的一次缓存查询；新注册模块会清除派发计划缓存。 */
    public static synchronized List<Delivery> subscribers(Class<? extends GhostEvent> type) {
        return RESOLVED.computeIfAbsent(type,GhostEventSubscriptions::resolve);
    }
    /** 按事件继承树做字典查找，不通过遍历所有声明做 isAssignableFrom 过滤。 */
    private static List<Delivery> resolve(Class<?> type) {
        Map<ResourceLocation,Builder> builders=new HashMap<>();
        collect(type,new HashSet<>(),builders);
        return builders.values().stream().sorted(Comparator.comparingInt(builder->builder.order)).map(Builder::build).toList();
    }
    /** 支持事件父接口、多重接口与去重，避免同一规则因多个订阅重复触发。 */
    private static void collect(Class<?> type,Set<Class<?>> visited,Map<ResourceLocation,Builder> builders) {
        if(type==null||!visited.add(type))return;
        for(Listener listener:BY_TYPE.getOrDefault(type,List.of()))
            builders.computeIfAbsent(listener.module().getId(),ignored->new Builder(listener)).add(listener);
        for(Class<?> parent:type.getInterfaces())collect(parent,visited,builders);
        collect(type.getSuperclass(),visited,builders);
    }
    /** 上下文直接按 ID 读取注册时的定义，不重新构造模块规则列表。 */
    public static synchronized GhostRule rule(ResourceLocation module,ResourceLocation rule) {
        return RULES.getOrDefault(module,Map.of()).get(rule);
    }
    /** 保留直接按模块派发的入口，但其规则仍来自订阅查询，不重建完整规则列表。 */
    public static List<GhostRule> rules(ResourceLocation module,Class<? extends GhostEvent> event) {
        for(Delivery delivery:subscribers(event))if(delivery.module().getId().equals(module))return delivery.rules();
        return List.of();
    }
}
