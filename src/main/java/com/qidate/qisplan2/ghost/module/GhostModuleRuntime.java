package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceSession;

import java.util.List;

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

        GhostInfluenceSession session =
                new GhostInfluenceSession();

        try {
            for (GhostModuleHost host : List.copyOf(hosts)) {
                if (host == null) continue;
                List<GhostModuleData.Entry> entries = List.copyOf(host.getModules());
                for (GhostModuleData.Entry entry : entries) {
                    if (!host.hasModule(entry.id())) continue;
                    GhostModule module = GhostModuleRegistry.get(entry.id());
                    if (module == null) continue;
                    GhostRuleRuntime.dispatchModule(host, entry, event, session);
                }
            }
        } finally {
            // 即使某条规则抛出异常，也不能遗留一个
            // 已经无人持有、却尚未完成的事件会话。
            session.resolveAndApply();
        }
    }
}
