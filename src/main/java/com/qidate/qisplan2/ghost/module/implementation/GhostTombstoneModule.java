package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.GhostUseEvent;
import net.minecraft.resources.ResourceLocation;

import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 鬼墓碑模块。
 *
 * 负责定义鬼墓碑对应的灵异模块身份，
 * 并响应通用灵异使用事件。
 */
public final class GhostTombstoneModule implements GhostModule {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_tombstone"
            );

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    /**
     * 定义鬼墓碑模块的新实例初始属性。
     */
    @Override
    public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                25.0D,
                0.0D,
                0.0D
        );
    }

    @Override
    public boolean supports(GhostEvent event) {
        return event instanceof GhostUseEvent;
    }

    @Override
    public void onEvent(
            GhostModuleContext context,
            GhostEvent event
    ) {
        if (!(event instanceof GhostUseEvent useEvent)) {
            return;
        }

        // 不可触规则每触发一次，当前模块实例的复苏值增加 1。
        context.setRevival(context.revival() + 1.0D);

        // 下一步接入墓碑宿主后，在这里实现刻字规则。
        // 物品标签判断属于模块规则，不属于通用事件分发器。
    }
}