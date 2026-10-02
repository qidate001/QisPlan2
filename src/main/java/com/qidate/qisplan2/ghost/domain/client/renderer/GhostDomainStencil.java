package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.isolation.client.renderer.GhostIsolationGpuData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
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

    private static final ResourceLocation SHADER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_isolation_stencil"
            );

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

    public static void writeIsolationMask() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        if (!GhostDomainShaderRegistry.isRegistered(SHADER_ID)) {
            return;
        }

        ShaderInstance shader =
                GhostDomainShaderRegistry.get(SHADER_ID);

        if (shader == null) {
            return;
        }

        var target =
                minecraft.getMainRenderTarget();

        /*
         * ========================================================
         * 确保 MainRenderTarget 有 Stencil。
         * ========================================================
         */
        if (!target.isStencilEnabled()) {
            target.enableStencil();
        }

        /*
         * ========================================================
         * 上传最新的 Cuboid GPU 数据。
         * ========================================================
         */
        GhostIsolationGpuData.uploadIfNeeded();

        /*
         * ========================================================
         * 设置 Stencil：
         *
         * 所有 Fragment 默认允许通过。
         *
         * 后面的 Shader：
         *
         *     return → REPLACE → Stencil = 1
         *
         *     discard → 不写入 Stencil
         * ========================================================
         */
        GL11.glEnable(GL11.GL_STENCIL_TEST);

        RenderSystem.stencilMask(0xFF);

        RenderSystem.stencilFunc(
                GL11.GL_ALWAYS,
                1,
                0xFF
        );

        RenderSystem.stencilOp(
                GL11.GL_KEEP,
                GL11.GL_KEEP,
                GL11.GL_REPLACE
        );

        /*
         * ========================================================
         * 不修改颜色。
         * ========================================================
         */
        RenderSystem.colorMask(
                false,
                false,
                false,
                false
        );

        /*
         * 不参与深度测试。
         */
        RenderSystem.disableDepthTest();

        /*
         * 不修改 Depth。
         */
        RenderSystem.depthMask(false);

        /*
         * ========================================================
         * 清空 Stencil。
         * ========================================================
         */
        RenderSystem.clearStencil(0);

        RenderSystem.clear(
                GL11.GL_STENCIL_BUFFER_BIT,
                Minecraft.ON_OSX
        );

        /*
         * ========================================================
         * 设置 Shader。
         * ========================================================
         */
        RenderSystem.setShader(
                () -> shader
        );

        shader.setSampler(
                "IsolationCuboidData",
                GhostIsolationGpuData.getTextureId()
        );

        shader.getUniform(
                "IsolationCuboidCount"
        ).set(
                (float) GhostIsolationGpuData.getCuboidCount()
        );

        /*
         * 使用共享 Depth。
         */
        shader.setSampler(
                "MainDepthSampler",
                GhostDomainDepthTarget
                        .get()
                        .getDepthTextureId()
        );

        /*
         * Projection Matrix。
         */
        shader.getUniform(
                "IsolationProjMat"
        ).set(
                RenderSystem.getProjectionMatrix()
        );

        /*
         * ModelView Matrix。
         */
        shader.getUniform(
                "IsolationModelViewMat"
        ).set(
                GhostDomainMatrices
                        .getModelViewMatrix()
        );

        /*
         * Camera Position。
         */
        var camera =
                minecraft.gameRenderer
                        .getMainCamera();

        shader.getUniform(
                "IsolationCameraPos"
        ).set(
                (float) camera.getPosition().x,
                (float) camera.getPosition().y,
                (float) camera.getPosition().z
        );

        /*
         * ========================================================
         * 绘制 Fullscreen Quad。
         * ========================================================
         */
        target.bindWrite(false);

        RenderSystem.viewport(
                0,
                0,
                target.width,
                target.height
        );

        drawFullscreenQuad();

        /*
         * ========================================================
         * 恢复颜色 / Depth。
         * ========================================================
         */
        RenderSystem.colorMask(
                true,
                true,
                true,
                true
        );

        RenderSystem.depthMask(true);

        RenderSystem.enableDepthTest();

        /*
         * ========================================================
         * 后续 GhostDomain Effect：
         *
         * Stencil = 0 → 允许
         * Stencil = 1 → 禁止
         * ========================================================
         */
        RenderSystem.stencilFunc(
                GL11.GL_EQUAL,
                0,
                0xFF
        );

        /*
         * 后续不再修改 Stencil。
         */
        RenderSystem.stencilMask(0x00);
    }

    /**
     * 绘制一个覆盖整个屏幕的 Quad。
     */
    public static void drawFullscreenQuad() {

        Minecraft minecraft =
                Minecraft.getInstance();

        var mainTarget =
                minecraft.getMainRenderTarget();

        /*
         * 后处理不需要深度测试。
         */
        RenderSystem.disableDepthTest();

        /*
         * 后处理不会向深度缓冲写入内容。
         */
        RenderSystem.depthMask(false);

        /*
         * 确保当前 viewport 对应主渲染目标。
         */
        RenderSystem.viewport(
                0,
                0,
                mainTarget.width,
                mainTarget.height
        );

        Tesselator tesselator =
                Tesselator.getInstance();

        BufferBuilder buffer =
                tesselator.begin(
                        VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION
                );

        buffer.addVertex(
                -1.0F,
                1.0F,
                0.0F
        );

        buffer.addVertex(
                -1.0F,
                -1.0F,
                0.0F
        );

        buffer.addVertex(
                1.0F,
                -1.0F,
                0.0F
        );

        buffer.addVertex(
                1.0F,
                1.0F,
                0.0F
        );

        BufferUploader.drawWithShader(
                buffer.buildOrThrow()
        );

        /*
         * 恢复后续世界/第一人称渲染需要的状态。
         */
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                1.0F
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