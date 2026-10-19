package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceSession;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 灵异规则运行时。
 *
 * 负责执行指定模块在当前事件中符合条件的规则。
 *
 * 职责范围：
 * 1. 检查模块是否允许执行规则。
 * 2. 获取模块及其规则列表。
 * 3. 检查规则是否支持当前事件，以及是否满足触发条件。
 * 4. 执行满足条件的规则，并将共享的影响会话传递给规则上下文。
 *
 * 本类不负责完整事件分发，不创建或裁决灵异影响会话，
 * 也不负责遍历宿主的全部模块。
 *
 * 完整事件分发由 GhostModuleRuntime 负责。
 */
public final class GhostRuleRuntime {

    private GhostRuleRuntime() {
    }

    /**
     * 执行指定模块在当前事件中符合条件的规则。
     *
     * 只负责规则筛选与执行，不负责创建会话或裁决影响。
     *
     * @param host 模块宿主
     * @param entry 当前模块的数据条目
     * @param event 当前事件
     * @param session 当前事件共享的灵异影响会话
     */
    public static void dispatchModule(
            GhostModuleHost host,
            GhostModuleData.Entry entry,
            GhostEvent event,
            GhostInfluenceSession session
    ) {
        if (host == null || entry == null
                || event == null || session == null) {
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
                            rule.getId(),
                            session
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

    /**
     * 检查模块是否存在，且没有沉睡或死机。
     */
    private static boolean canRunRules(
            GhostModuleHost host,
            ResourceLocation moduleId
    ) {
        if (!host.hasModule(moduleId)) {
            return false;
        }

        for (GhostModuleData.Entry currentEntry : host.getModules()) {
            if (currentEntry.id().equals(moduleId)) {
                return !currentEntry.sleeping()
                        && !currentEntry.deadlocked();
            }
        }

        return false;
    }
}