package com.qidate.qisplan2.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;

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

        // 当前没有游戏世界时不渲染
        if (minecraft.level == null) {
            return;
        }

        // 使用我们的 Shader
        RenderSystem.setShader(GhostEyeShader::getInstance);

        // 暂时关闭深度测试，确保 Quad 覆盖整个屏幕
        RenderSystem.disableDepthTest();

        // 暂时不使用深度写入
        RenderSystem.depthMask(false);

        // 绘制全屏 Quad
        Tesselator tesselator = Tesselator.getInstance();

        BufferBuilder buffer = tesselator.begin(
                VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION
        );

        buffer.addVertex(-1.0F,  1.0F, 0.0F);
        buffer.addVertex(-1.0F, -1.0F, 0.0F);
        buffer.addVertex( 1.0F, -1.0F, 0.0F);
        buffer.addVertex( 1.0F,  1.0F, 0.0F);

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        // 恢复状态
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}