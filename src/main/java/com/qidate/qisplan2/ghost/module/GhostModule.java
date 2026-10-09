package com.qidate.qisplan2.ghost.module;

import net.minecraft.resources.ResourceLocation;

/**
 * 厉鬼模块的基础接口。
 *
 * 每种厉鬼模块都需要提供唯一 ID。
 * 具体规则由后续的模块实现负责。
 */
public interface GhostModule {

    /**
     * 获取模块的唯一标识。
     */
    ResourceLocation getId();
}