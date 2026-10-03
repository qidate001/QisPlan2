package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.isolation.client.renderer.GhostIsolationGpuData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * 鬼域 Stencil 管理器。
 *
 * <p>
 * 负责根据 {@link GhostIsolationGpuData} 中的灵异隔绝空间数据，
 * 在主渲染目标的 Stencil Buffer 中生成隔绝区域标记。
 * </p>
 *
 * <p>
 * Stencil 值约定：
 * </p>
 *
 * <ul>
 *     <li>{@code 0}：正常空间，允许鬼域后处理</li>
 *     <li>{@code 1}：灵异隔绝空间，禁止鬼域后处理</li>
 * </ul>
 *
 * <p>
 * GhostDomain 后处理在 Stencil Test 下进行渲染，
 * 因此各个具体的鬼域效果无需自行判断灵异隔绝空间。
 * </p>
 */
public final class GhostDomainStencil {

    /**
     * 用于生成隔绝空间 Stencil 的 Shader。
     */
    private static final ResourceLocation SHADER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_isolation_stencil"
            );

    private GhostDomainStencil() {
    }

    /**
     * 根据当前灵异隔绝空间数据生成 Stencil Mask。
     *
     * <p>
     * Shader 会对屏幕上的每一个像素进行计算，
     * 判断摄像机到当前可见世界表面的射线是否经过
     * 任意一个 {@code GhostIsolationCuboid}。
     * </p>
     *
     * <p>
     * 对于处于灵异隔绝空间的像素：
     * </p>
     *
     * <pre>
     * Fragment Shader 正常输出
     *         ↓
     * Stencil Operation = REPLACE
     *         ↓
     * Stencil = 1
     * </pre>
     *
     * <p>
     * 对于不处于灵异隔绝空间的像素：
     * </p>
     *
     * <pre>
     * Fragment Shader discard
     *         ↓
     * 不执行 Stencil 写入
     *         ↓
     * Stencil 保持为 0
     * </pre>
     *
     * <p>
     * 完成后，Stencil Test 会被设置为：
     * </p>
     *
     * <pre>
     * Stencil == 0 → 允许后续鬼域效果
     * Stencil == 1 → 禁止后续鬼域效果
     * </pre>
     */
    public static void writeIsolationStencil() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        /*
         * Shader 尚未注册时，
         * 当前帧无法生成 Stencil。
         */
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
         * 确保主渲染目标拥有 Stencil Buffer。
         * ========================================================
         */
        if (!target.isStencilEnabled()) {
            target.enableStencil();
        }

        /*
         * ========================================================
         * 上传最新的灵异隔绝空间 GPU 数据。
         * ========================================================
         *
         * CPU 侧数据可能已经发生变化，
         * 这里只负责确保 Render Thread 上的 GPU Texture
         * 已经同步到最新状态。
         */
        GhostIsolationGpuData.uploadIfNeeded();

        /*
         * ========================================================
         * 开启 Stencil Test。
         * ========================================================
         */
        GL11.glEnable(GL11.GL_STENCIL_TEST);

        /*
         * 允许写入全部 Stencil 位。
         */
        RenderSystem.stencilMask(0xFF);

        /*
         * 所有 Fragment 默认通过 Stencil Test。
         */
        RenderSystem.stencilFunc(
                GL11.GL_ALWAYS,
                1,
                0xFF
        );

        /*
         * Fragment Shader 正常通过时，
         * 将 Stencil 写成 1。
         *
         * Shader 使用 discard 时，
         * 不会执行 Stencil 写入。
         */
        RenderSystem.stencilOp(
                GL11.GL_KEEP,
                GL11.GL_KEEP,
                GL11.GL_REPLACE
        );

        /*
         * ========================================================
         * Stencil 生成阶段不修改颜色。
         * ========================================================
         */
        RenderSystem.colorMask(
                false,
                false,
                false,
                false
        );

        /*
         * 不参与 Depth Test。
         *
         * Stencil 的生成完全由隔绝空间 Shader 决定，
         * 不应该受到当前世界 Depth 的影响。
         */
        RenderSystem.disableDepthTest();

        /*
         * 不修改 Depth Buffer。
         */
        RenderSystem.depthMask(false);

        /*
         * ========================================================
         * 清空上一帧的 Stencil。
         * ========================================================
         */
        target.bindWrite(false);

        RenderSystem.viewport(
                0,
                0,
                target.width,
                target.height
        );

        RenderSystem.clearStencil(0);

        RenderSystem.clear(
                GL11.GL_STENCIL_BUFFER_BIT,
                Minecraft.ON_OSX
        );

        /*
         * ========================================================
         * 设置隔绝空间 Stencil Shader。
         * ========================================================
         */

        RenderSystem.setShader(
                () -> shader
        );

        /*
         * 灵异隔绝空间 Cuboid GPU Texture。
         */
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
         * 使用 GhostDomain 共享 Depth。
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
         * 执行隔绝空间 Stencil 生成。
         * ========================================================
         *
         * GhostDomainRenderPipeline 负责通用的
         * Fullscreen Quad 绘制。
         */
        GhostDomainRenderPipeline.drawFullscreenQuad();

        /*
         * ========================================================
         * 恢复颜色 / Depth 状态。
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
         * 配置后续 GhostDomain Effect 的 Stencil Test。
         * ========================================================
         *
         * Stencil = 0：
         *     正常空间 → 允许渲染
         *
         * Stencil = 1：
         *     灵异隔绝空间 → 禁止渲染
         */
        RenderSystem.stencilFunc(
                GL11.GL_EQUAL,
                0,
                0xFF
        );

        /*
         * 后续鬼域效果只读取 Stencil，
         * 不再修改 Stencil Buffer。
         */
        RenderSystem.stencilMask(0x00);
    }

    /**
     * 恢复 Stencil 默认状态。
     *
     * <p>
     * 防止 GhostDomain 后处理结束后，
     * Stencil Test 状态继续影响 Minecraft 后续渲染。
     * </p>
     */
    public static void disable() {

        /*
         * 恢复 Stencil 写入。
         */
        RenderSystem.stencilMask(0xFF);

        /*
         * 恢复默认 Stencil Test：
         * 所有像素都允许通过。
         */
        RenderSystem.stencilFunc(
                GL11.GL_ALWAYS,
                0,
                0xFF
        );

        /*
         * 后续不再执行 Stencil 修改。
         */
        RenderSystem.stencilOp(
                GL11.GL_KEEP,
                GL11.GL_KEEP,
                GL11.GL_KEEP
        );

        /*
         * 关闭 Stencil Test。
         */
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
}