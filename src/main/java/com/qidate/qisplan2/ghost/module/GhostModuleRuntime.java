package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;

import java.util.List;

/**
 * 统一的厉鬼模块运行时。
 *
 * 物品和实体均通过此处分发事件。
 *
 * 已迁移模块使用 GhostRuleRuntime；
 * 尚未迁移模块继续使用旧 supports/onEvent 接口。
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
            GhostModule module =
                    GhostModuleRegistry.get(entry.id());

            if (module == null) {
                continue;
            }

            if (module.usesRuleRuntime()) {
                GhostRuleRuntime.dispatchModule(
                        host,
                        entry,
                        event
                );
                continue;
            }

            // 未迁移模块继续使用原有事件接口。
            if (!module.supports(event)) {
                continue;
            }

            module.onEvent(
                    new GhostModuleContext(host, entry.id()),
                    event
            );
        }
    }
}