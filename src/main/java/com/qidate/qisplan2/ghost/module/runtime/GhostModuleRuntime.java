package com.qidate.qisplan2.ghost.module.runtime;

import com.qidate.qisplan2.ghost.module.GhostItemData;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleData;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 厉鬼模块运行时。
 *
 * 负责读取物品携带的模块，并将事件分发给对应实现。
 */
public final class GhostModuleRuntime {

    private GhostModuleRuntime() {
    }

    /**
     * 将事件分发给物品上所有订阅该事件的模块。
     */
    public static void dispatch(
            ItemStack stack,
            GhostEvent event
    ) {
        if (stack.isEmpty()) {
            return;
        }

        List<GhostModuleData.Entry> entries =
                GhostItemData.getModules(stack);

        for (GhostModuleData.Entry entry : entries) {
            GhostModule module =
                    GhostModuleRegistry.get(entry.id());

            // 未注册的模块暂时跳过，避免阻断其他模块。
            if (module == null) {
                continue;
            }

            if (!module.supports(event)) {
                continue;
            }

            module.onEvent(event, entry.intensity());
        }
    }
}