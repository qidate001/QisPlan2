package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.influence.GhostAttackContext;
import com.qidate.qisplan2.ghost.module.event.SupernaturalAttackEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostAttackResponses;
import com.qidate.qisplan2.ghost.module.sound.GhostModuleSoundManager;
import com.qidate.qisplan2.ghost.module.event.ModuleTickEvent;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceSession;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import net.minecraft.resources.ResourceLocation;
import com.qidate.qisplan2.ghost.module.host.ModuleGhostDomains;

/**
 * 灵异模块运行时。
 *
 * 负责完整的模块事件分发流程：
 * 1. 为一次事件创建共享的 GhostInfluenceSession。
 * 2. 从事件订阅索引查询实际接收者。
 * 3. 将各模块交给 GhostRuleRuntime 执行规则。
 * 4. 所有模块执行完成后，统一裁决并应用灵异影响。
 *
 * 这是模块事件分发的统一入口。
 */
public final class GhostModuleRuntime {

    /**
     * 对抗管线已经完成响应后，只向沉睡模块报告事实。
     * 不再次执行抵挡、持续能力或正常规则；即使本次惊醒也不会参与本次反击。
     */
    public static void observeSleeping(List<? extends GhostModuleHost> hosts, GhostEvent event) {
        var session = new GhostInfluenceSession();
        try {
            for (var host : hosts) for (var recipient : recipients(host,event)) {
                var entry=recipient.entry();
                if(entry.sleeping())GhostRuleRuntime.dispatchSubscribed(host,entry,event,session,recipient.delivery().rules());
            }
        } finally { session.resolveAndApply(); }
    }

    private GhostModuleRuntime() {
    }

    /**
     * 向宿主携带的模块分发事件。
     *
     * 同一次分发只创建一个影响会话：
     * 1. 分发所有模块规则。
     * 2. 统一裁决已提交的灵异影响。
     * 3. 应用裁决结果并执行获准效果。
     */
    public static void dispatch(
            GhostModuleHost host,
            GhostEvent event
    ) {
        if (host == null || event == null) {
            return;
        }

        dispatch(List.of(host), event);
    }

    /**
     * 在同一个共享影响批次中，将一个世界事件派发给多个主机。
     */
    public static void dispatch(
            List<? extends GhostModuleHost> hosts,
            GhostEvent event
    ) {
        if (hosts == null || event == null || hosts.isEmpty()) {
            return;
        }

        if (event instanceof SupernaturalAttackEvent attack) {
            var context = new GhostAttackContext(
                    attack, null, () -> {}, attack::remainingIntensity);
            GhostAttackResponses.apply(
                    GhostAttackResponses.collect(hosts), context);
        }
        GhostInfluenceSession session =
                new GhostInfluenceSession();

        try {
            for (GhostModuleHost host : List.copyOf(hosts)) {
                if (host == null) continue;
                if (event instanceof ModuleTickEvent) {
                    GhostModuleCombatState.tick(host);
                    if (host.controlsIntrinsicModules()) {
                        var lost = host.getModules().stream().filter(entry -> {
                            var definition = GhostModuleRegistry.get(entry.id());
                            return definition != null && !entry.sleeping() && !entry.deadlocked()
                                    && host.carriesModule(entry.id()) && entry.revival() >= definition.getMaxRevival();
                        }).findFirst();
                        if (lost.isPresent()) { host.onControlLost(lost.get().id()); continue; }
                    }
                }
                if (event instanceof ModuleTickEvent)
                    GhostModuleSoundManager.syncCarriedHost(host);
                if (event instanceof ModuleTickEvent) ModuleGhostDomains.sync(host);
                for (Recipient recipient : recipients(host,event)) {
                    var entry=recipient.entry();var delivery=recipient.delivery();var module=delivery.module();
                    if (!host.hasModule(entry.id())) continue;
                    if(delivery.abilityEvent() && abilitiesAvailable(host,entry))
                        module.onAbilityEvent(new GhostModuleContext(host,entry.id()),event);
                    if(!host.hasModule(entry.id()))continue;
                    if(delivery.abilityTick() && abilitiesAvailable(host,entry))
                        module.tickAbilities(new GhostModuleContext(host,entry.id()));
                    if(host.hasModule(entry.id()))GhostRuleRuntime.dispatchSubscribed(host,entry,event,session,delivery.rules());
                }
            }
        } finally {
            // 即使某条规则抛出异常，也不能遗留一个
            // 已经无人持有、却尚未完成的事件会话。
            session.resolveAndApply();
        }
    }
    /** 宿主数据快照只用于安装关系查询；不向未订阅模块调用任何事件方法。 */
    private record Recipient(GhostModuleData.Entry entry,GhostEventSubscriptions.Delivery delivery,int order) {}

    /** 先查事件索引，再与宿主安装关系相交；保持原有宿主模块顺序。 */
    private static List<Recipient> recipients(GhostModuleHost host,GhostEvent event) {
        var deliveries=GhostEventSubscriptions.subscribers(event.getClass());
        if(deliveries.isEmpty())return List.of();
        var entries=List.copyOf(host.getModules());
        var positions=new HashMap<ResourceLocation,Integer>();
        for(int i=0;i<entries.size();i++)positions.put(entries.get(i).id(),i);
        var result=new ArrayList<Recipient>();
        for(var delivery:deliveries) {
            var position=positions.get(delivery.module().getId());
            if(position!=null)result.add(new Recipient(entries.get(position),delivery,position));
        }
        result.sort(Comparator.comparingInt(Recipient::order));
        return result;
    }
    /** 持续能力与规则各自检查状态，死机只阻止规则。 */
    private static boolean abilitiesAvailable(GhostModuleHost host,GhostModuleData.Entry snapshot) {
        var entry=host.getModules().stream().filter(e->e.id().equals(snapshot.id())).findFirst().orElse(null);
        return entry!=null && (!entry.sleeping()||host.controlsIntrinsicModules()) && host.carriesModule(entry.id())
                && host.suppression(entry.id()).canUseAbility(host.controlsIntrinsicModules());
    }

}
