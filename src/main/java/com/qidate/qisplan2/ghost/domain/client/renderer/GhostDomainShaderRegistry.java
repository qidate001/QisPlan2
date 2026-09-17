package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class GhostDomainShaderRegistry {

    private static final Map<ResourceLocation, ShaderInstance> SHADERS =
            new HashMap<>();

    private GhostDomainShaderRegistry() {
    }

    /**
     * 注册一个鬼域 Shader。
     */
    public static void register(
            ResourceLocation id,
            ShaderInstance shader
    ) {

        SHADERS.put(
                id,
                shader
        );

        QisPlan2.LOGGER.info(
                "[鬼域渲染] Shader 注册成功: {}",
                id
        );
    }

    /**
     * 获取指定鬼域 Shader。
     */
    public static ShaderInstance get(
            ResourceLocation id
    ) {

        return SHADERS.get(id);
    }

    /**
     * 判断指定鬼域 Shader 是否已经注册。
     */
    public static boolean isRegistered(
            ResourceLocation id
    ) {

        return SHADERS.containsKey(id);
    }

    /**
     * 清空所有 Shader。
     *
     * <p>
     * 目前主要用于客户端资源重载等场景。
     * </p>
     */
    public static void clear() {

        SHADERS.clear();
    }
}