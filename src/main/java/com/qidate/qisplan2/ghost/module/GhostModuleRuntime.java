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
import com.qidate.qisplan2.ghost.module.host.ModuleGhostDomains;

/**
 * 灵异模块运行时。
 *
 * 负责完整的模块事件分发流程：
 * 1. 为一次事件创建共享的 GhostInfluenceSession。
 * 2. 遍历宿主当前拥有的模块。
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
            for (var host : hosts) for (var entry : List.copyOf(host.getModules()))
                if (entry.sleeping()) GhostRuleRuntime.dispatchModule(host, entry, event, session);
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
                List<GhostModuleData.Entry> entries = List.copyOf(host.getModules());
                for (GhostModuleData.Entry entry : entries) {
                    if (!host.hasModule(entry.id())) continue;
                    GhostModule module = GhostModuleRegistry.get(entry.id());
                    if (module == null) continue;
                    if ((!entry.sleeping() || host.controlsIntrinsicModules()) && host.carriesModule(entry.id()) && host.suppression(entry.id()).canUseAbility(host.controlsIntrinsicModules())) module.onAbilityEvent(new GhostModuleContext(host, entry.id()), event);
                    if (!host.hasModule(entry.id())) continue;
                    if (event instanceof ModuleTickEvent && (!entry.sleeping() || host.controlsIntrinsicModules()) && host.carriesModule(entry.id())
                            && host.suppression(entry.id()).canUseAbility(host.controlsIntrinsicModules()))
                        module.tickAbilities(new GhostModuleContext(host, entry.id()));
                    if (host.hasModule(entry.id())) GhostRuleRuntime.dispatchModule(host, entry, event, session);
                }
            }
        } finally {
            // 即使某条规则抛出异常，也不能遗留一个
            // 已经无人持有、却尚未完成的事件会话。
            session.resolveAndApply();
        }
    }
}
