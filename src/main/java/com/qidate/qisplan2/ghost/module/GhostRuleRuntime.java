package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;

import java.util.List;

/**
 * 统一的厉鬼规则运行时。
 *
 * 负责将语义事件分发给宿主所持有模块定义中的规则。
 *
 * 当前阶段：
 * 1. 获取宿主模块快照。
 * 2. 获取模块定义中的规则快照。
 * 3. 筛选支持当前事件的规则。
 * 4. 检查规则前置条件。
 * 5. 执行满足条件的规则。
 *
 * 规则与能力的执行逻辑保持分离。
 */
public final class GhostRuleRuntime {

    private GhostRuleRuntime() {
    }

    /**
     * 向宿主持有的所有模块规则分发事件。
     */
    public static void dispatch(
            GhostModuleHost host,
            GhostEvent event
    ) {
        if (host == null || event == null) {
            return;
        }

        // 固定本次派发开始时的模块列表。
        List<GhostModuleData.Entry> moduleEntries =
                List.copyOf(host.getModules());

        for (GhostModuleData.Entry entry : moduleEntries) {
            GhostModule module =
                    GhostModuleRegistry.get(entry.id());

            if (module == null) {
                continue;
            }

            // 固定当前模块的规则列表，避免遍历期间列表变化。
            List<GhostRule> rules =
                    List.copyOf(module.getRules());

            for (GhostRule rule : rules) {
                if (!rule.supports(event)) {
                    continue;
                }

                GhostRuleContext context =
                        new GhostRuleContext(
                                host,
                                entry.id(),
                                rule.getId()
                        );

                if (!rule.canTrigger(context, event)) {
                    continue;
                }

                rule.onTrigger(context, event);
            }
        }
    }

    /**
     * 只向指定模块的规则分发事件。
     */
    public static void dispatchModule(
            GhostModuleHost host,
            GhostModuleData.Entry entry,
            GhostEvent event
    ) {
        if (host == null || entry == null || event == null) {
            return;
        }

        GhostModule module = GhostModuleRegistry.get(entry.id());

        if (module == null) {
            return;
        }

        List<GhostRule> rules = List.copyOf(module.getRules());

        for (GhostRule rule : rules) {
            if (!rule.supports(event)) {
                continue;
            }

            GhostRuleContext context =
                    new GhostRuleContext(
                            host,
                            entry.id(),
                            rule.getId()
                    );

            if (!rule.canTrigger(context, event)) {
                continue;
            }

            rule.onTrigger(context, event);
        }
    }
}