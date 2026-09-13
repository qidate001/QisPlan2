package com.qidate.qisplan2.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

public final class GhostEyeRenderHandler {

    private GhostEyeRenderHandler() {
    }

    /**
     * 当前阶段仅用于测试：
     *
     * 世界渲染完成后绘制一个覆盖整个屏幕的 Quad。
     * Shader 本身会将它渲染成纯红色。
     */
    public static void render() {

        if (!GhostEyeShader.isReady()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        ShaderInstance shader = GhostEyeShader.getInstance();

        RenderSystem.setShader(() -> shader);

        int colorTexture =
                minecraft.getMainRenderTarget()
                        .getColorTextureId();

        shader.setSampler(
                "DiffuseSampler",
                colorTexture
        );

        int depthTexture =
                minecraft.getMainRenderTarget()
                        .getDepthTextureId();

        shader.setSampler(
                "MainDepthSampler",
                depthTexture
        );

        shader.getUniform("ProjMat").set(
                RenderSystem.getProjectionMatrix()
        );

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Tesselator tesselator =
                Tesselator.getInstance();

        BufferBuilder buffer =
                tesselator.begin(
                        VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION
                );

        buffer.addVertex(-1.0F,  1.0F, 0.0F);
        buffer.addVertex(-1.0F, -1.0F, 0.0F);
        buffer.addVertex( 1.0F, -1.0F, 0.0F);
        buffer.addVertex( 1.0F,  1.0F, 0.0F);

        BufferUploader.drawWithShader(
                buffer.buildOrThrow()
        );

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}