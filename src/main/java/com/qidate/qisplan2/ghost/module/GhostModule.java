package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.resources.ResourceLocation;

/**
 * 灵异模块定义。
 *
 * 模块负责声明自身的身份、初始属性，
 * 以及支持哪些事件、收到事件后执行什么规则。
 *
 * 初始属性只用于创建新的模块实例，
 * 不代表已经存在的实例当前保存的属性。
 */
public interface GhostModule {

    /**
     * 获取模块的唯一标识。
     */
    ResourceLocation getId();

    /**
     * 获取模块定义提供的初始属性。
     *
     * 默认值：
     * - 灵异强度：1.0
     * - 复苏值：0.0
     * - 死机值：0.0
     *
     * 具体模块可以覆盖此方法，定义自己的初始属性。
     */
    default GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                1.0D,
                0.0D,
                0.0D
        );
    }

    /**
     * 判断模块是否支持指定事件。
     */
    boolean supports(GhostEvent event);

    /**
     * 执行模块的事件规则。
     */
    void onEvent(GhostModuleContext context, GhostEvent event);
}