package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;

import java.util.List;

/**
 * 统一的厉鬼规则运行时。
 *
 * 负责将语义事件分发给宿主所持有模块定义中的规则。
 *
 * 每次分发固定模块列表与规则列表，避免遍历期间集合变化。
 * 每次执行规则前检查模块是否仍然存在。
 * 已被移除或处于沉睡状态的模块不执行自身规则。
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

        List<GhostModuleData.Entry> moduleEntries =
                List.copyOf(host.getModules());

        for (GhostModuleData.Entry entry : moduleEntries) {
            if (!canRunRules(host, entry.id())) {
                continue;
            }

            GhostModule module =
                    GhostModuleRegistry.get(entry.id());

            if (module == null) {
                continue;
            }

            List<GhostRule> rules =
                    List.copyOf(module.getRules());

            for (GhostRule rule : rules) {
                // 模块可能在之前的规则执行期间被移除或进入沉睡。
                if (!canRunRules(host, entry.id())) {
                    break;
                }

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

                // canTrigger 也可能改变模块状态。
                if (!canRunRules(host, entry.id())) {
                    break;
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

        if (!canRunRules(host, entry.id())) {
            return;
        }

        GhostModule module =
                GhostModuleRegistry.get(entry.id());

        if (module == null) {
            return;
        }

        List<GhostRule> rules =
                List.copyOf(module.getRules());

        for (GhostRule rule : rules) {
            if (!canRunRules(host, entry.id())) {
                break;
            }

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

            if (!canRunRules(host, entry.id())) {
                break;
            }

            rule.onTrigger(context, event);
        }
    }

    /**
     * 检查模块是否存在且没有沉睡。
     *
     * 每次从宿主读取最新模块数据，避免使用旧快照中的状态。
     */
    private static boolean canRunRules(
            GhostModuleHost host,
            net.minecraft.resources.ResourceLocation moduleId
    ) {
        if (!host.hasModule(moduleId)) {
            return false;
        }

        for (GhostModuleData.Entry currentEntry : host.getModules()) {
            if (currentEntry.id().equals(moduleId)) {
                return !currentEntry.sleeping();
            }
        }

        return false;
    }
}