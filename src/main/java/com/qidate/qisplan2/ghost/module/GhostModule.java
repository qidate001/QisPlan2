package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.resources.ResourceLocation;

/**
 * 统一的厉鬼模块。
 *
 * 不区分物品宿主和实体宿主。
 */
public interface GhostModule {

    ResourceLocation getId();

    /**
     * 判断模块是否订阅指定事件。
     */
    boolean supports(GhostEvent event);

    /**
     * 处理模块事件。
     */
    void onEvent(
            GhostModuleContext context,
            GhostEvent event
    );
}