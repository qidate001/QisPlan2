package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.resources.ResourceLocation;

/**
 * 厉鬼模块基础接口。
 */
public interface GhostModule {

    /**
     * 获取模块唯一标识。
     */
    ResourceLocation getId();

    /**
     * 判断模块是否订阅指定事件。
     */
    boolean supports(GhostEvent event);

    /**
     * 处理模块订阅的事件。
     *
     * 灵异强度等模块数据由运行时传入。
     */
    void onEvent(
            GhostEvent event,
            double intensity
    );
}