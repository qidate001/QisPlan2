package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;

import java.util.List;

/**
 * 统一的厉鬼模块运行时。
 *
 * 物品和实体均通过此处分发事件。
 */
public final class GhostModuleRuntime {

    private GhostModuleRuntime() {
    }

    public static void dispatch(
            GhostModuleHost host,
            GhostEvent event
    ) {
        if (host == null || event == null) {
            return;
        }

        List<GhostModuleData.Entry> entries =
                List.copyOf(host.getModules());

        for (GhostModuleData.Entry entry : entries) {
            GhostModule module =
                    GhostModuleRegistry.get(entry.id());

            if (module == null || !module.supports(event)) {
                continue;
            }

            module.onEvent(
                    new GhostModuleContext(host, entry.intensity()),
                    event
            );
        }
    }
}