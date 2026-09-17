package com.qidate.qisplan2.ghost.domain.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

public interface GhostDomainRenderEffect {

    /**
     * 当前渲染效果使用的 Shader ID。
     */
    ResourceLocation shaderId();

    /**
     * 判断当前是否应该渲染这个效果。
     */
    boolean shouldRender(
            Minecraft minecraft
    );

    /**
     * 设置该渲染效果专属的 Shader Uniform。
     */
    void setupUniforms(
            ShaderInstance shader,
            Minecraft minecraft
    );
}