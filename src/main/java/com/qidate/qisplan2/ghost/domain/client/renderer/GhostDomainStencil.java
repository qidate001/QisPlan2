package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/**
 * 鬼域 Stencil 测试。
 *
 * <p>
 * 当前阶段仅用于验证 Stencil Buffer 是否能够
 * 正常控制后续鬼域后处理的绘制区域。
 * </p>
 */
public final class GhostDomainStencil {

    private GhostDomainStencil() {
    }

    /**
     * 清空当前 MainRenderTarget 的 Stencil，
     * 然后在屏幕左半边写入 1。
     *
     * <p>
     * 当前仅用于测试，后续会替换为真正的
     * 灵异隔绝区域生成逻辑。
     * </p>
     */
    public static void writeTestMask() {

        Minecraft minecraft =
                Minecraft.getInstance();

        var target =
                minecraft.getMainRenderTarget();

        /*
         * 确保当前目标拥有 Stencil。
         */
        if (!target.isStencilEnabled()) {
            target.enableStencil();
        }

        target.bindWrite(false);

        RenderSystem.viewport(
                0,
                0,
                target.width,
                target.height
        );

        /*
         * 清空 Stencil Buffer。
         */
        RenderSystem.clearStencil(0);

        RenderSystem.clear(
                GL11.GL_STENCIL_BUFFER_BIT,
                Minecraft.ON_OSX
        );

        /*
         * 开启 Stencil Test。
         */
        GL11.glEnable(GL11.GL_STENCIL_TEST);

        /*
         * 允许写入全部 Stencil 位。
         */
        RenderSystem.stencilMask(0xFF);

        /*
         * 所有像素都允许通过 Stencil Test。
         */
        RenderSystem.stencilFunc(
                GL11.GL_ALWAYS,
                1,
                0xFF
        );

        /*
         * Stencil Test + Depth Test 都通过时，
         * 把 Stencil 写成 1。
         */
        RenderSystem.stencilOp(
                GL11.GL_KEEP,
                GL11.GL_KEEP,
                GL11.GL_REPLACE
        );

        /*
         * 写 Stencil 测试时不参与 Depth Test。
         *
         * 我们只是向 Stencil Buffer 写入测试区域，
         * 不应该受到已经存在的世界深度影响。
         */
        RenderSystem.disableDepthTest();

        /*
         * 不修改颜色。
         */
        RenderSystem.colorMask(
                false,
                false,
                false,
                false
        );

        /*
         * 不修改 Depth。
         */
        RenderSystem.depthMask(false);

        /*
         * 只绘制屏幕左半边。
         */
        drawLeftHalf(target.width, target.height);

        /*
         * 恢复颜色写入。
         */
        RenderSystem.colorMask(
                true,
                true,
                true,
                true
        );

        /*
         * 恢复 Depth 写入。
         */
        RenderSystem.depthMask(true);

        /*
         * 恢复 Depth Test。
         */
        RenderSystem.enableDepthTest();

        /*
         * 后续绘制只允许 Stencil == 1。
         */
        RenderSystem.stencilFunc(
                GL11.GL_EQUAL,
                1,
                0xFF
        );

        /*
         * 后续不再修改 Stencil。
         */
        RenderSystem.stencilMask(0x00);
    }

    /**
     * 绘制屏幕左半边。
     */
    private static void drawLeftHalf(
            int width,
            int height
    ) {
        float left =
                -1.0F;

        float right =
                0.0F;

        Tesselator tesselator =
                Tesselator.getInstance();

        BufferBuilder buffer =
                tesselator.begin(
                        VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION
                );

        buffer.addVertex(
                left,
                1.0F,
                0.0F
        );

        buffer.addVertex(
                left,
                -1.0F,
                0.0F
        );

        buffer.addVertex(
                right,
                -1.0F,
                0.0F
        );

        buffer.addVertex(
                right,
                1.0F,
                0.0F
        );

        BufferUploader.drawWithShader(
                buffer.buildOrThrow()
        );
    }

    /**
     * 恢复 Stencil 默认状态。
     */
    public static void disable() {

        RenderSystem.stencilMask(0xFF);

        RenderSystem.stencilFunc(
                GL11.GL_ALWAYS,
                0,
                0xFF
        );

        RenderSystem.stencilOp(
                GL11.GL_KEEP,
                GL11.GL_KEEP,
                GL11.GL_KEEP
        );

        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
}