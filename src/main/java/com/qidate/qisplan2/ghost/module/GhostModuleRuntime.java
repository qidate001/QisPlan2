package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;

import java.util.List;

/**
 * 统一的厉鬼模块运行时。
 *
 * 物品和实体均通过此处分发事件。
 * 所有模块统一使用 GhostRuleRuntime 处理事件。
 */
public final class GhostModuleRuntime {

    private GhostModuleRuntime() {
    }

    /**
     * 向宿主携带的模块分发事件。
     */
    public static void dispatch(
            GhostModuleHost host,
            GhostEvent event
    ) {
        if (host == null || event == null) {
            return;
        }

        // 固定本次派发开始时的模块列表。
        List<GhostModuleData.Entry> entries =
                List.copyOf(host.getModules());

        for (GhostModuleData.Entry entry : entries) {
            // 模块可能已在本次派发过程中被移除。
            if (!host.hasModule(entry.id())) {
                continue;
            }

            GhostModule module =
                    GhostModuleRegistry.get(entry.id());

            if (module == null) {
                continue;
            }

            GhostRuleRuntime.dispatchModule(
                    host,
                    entry,
                    event
            );
        }
    }
}