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

        // 创建快照，避免遍历过程中模块列表变化影响本次分发。
        List<GhostModuleData.Entry> entries =
                List.copyOf(host.getModules());

        for (GhostModuleData.Entry entry : entries) {
            GhostModule module =
                    GhostModuleRegistry.get(entry.id());

            if (module == null || !module.supports(event)) {
                continue;
            }

            // 传入模块 ID，由 Context 实时读取该模块实例的数据。
            module.onEvent(
                    new GhostModuleContext(host, entry.id()),
                    event
            );
        }
    }
}