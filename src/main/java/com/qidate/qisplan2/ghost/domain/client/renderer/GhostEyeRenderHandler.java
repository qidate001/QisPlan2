package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

public final class GhostEyeRenderHandler {

    private GhostEyeRenderHandler() {
    }

    /**
     * 鬼眼屏幕后处理。
     *
     * <p>该渲染发生在世界渲染完成后。</p>
     *
     * <p>这里会重建世界坐标，再根据鬼域范围决定是否应用鬼眼效果。</p>
     */
    public static void render() {

        /*
         * ========================================================
         * 基础检查
         * ========================================================
         */

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        GhostEyeRenderEffect effect =
                GhostEyeRenderEffect.getInstance();

        if (!effect.shouldRender(
                minecraft
        )) {
            return;
        }

        ResourceLocation shaderId =
                effect.shaderId();

        if (!GhostDomainShaderRegistry.isRegistered(
                shaderId
        )) {
            return;
        }

        /*
         * ========================================================
         * 准备 RenderTarget
         * ========================================================
         */

        GhostDomainRenderPipeline.prepareDepth();

        /*
         * ========================================================
         * 配置 Shader
         * ========================================================
         */

        ShaderInstance shader =
                GhostDomainShaderRegistry.get(
                        shaderId
                );

        GhostDomainRenderPipeline.setupShader(
                shader
        );

        effect.setupUniforms(
                shader,
                minecraft
        );

        /*
         * ========================================================
         * 绘制全屏 Quad
         * ========================================================
         */

        GhostDomainRenderPipeline.drawFullscreenQuad();
    }
}