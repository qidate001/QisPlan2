package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

public final class GhostDomainRenderPipeline {

    private GhostDomainRenderPipeline() {
    }

    /**
     * 执行一个完整的鬼域后处理效果。
     *
     * <p>
     * 该方法包含完整流程：
     * 判断 → Shader → 深度准备 → Shader 配置 → 绘制。
     * </p>
     */
    public static void render(
            GhostDomainRenderEffect effect
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        /*
         * ========================================================
         * 判断当前效果是否需要渲染
         * ========================================================
         */

        if (!effect.shouldRender(
                minecraft
        )) {
            return;
        }

        /*
         * ========================================================
         * 准备深度
         * ========================================================
         */

        prepareDepth();

        /*
         * ========================================================
         * 执行已经准备好深度的后处理
         * ========================================================
         */

        renderPrepared(
                effect,
                minecraft
        );
    }

    /**
     * 执行一个已经完成深度准备的鬼域后处理效果。
     *
     * <p>
     * 调用此方法之前，调用者必须已经执行：
     * {@link #prepareDepth()}
     * </p>
     *
     * <p>
     * 该方法不会再次复制深度。
     * </p>
     */
    public static void renderPrepared(
            GhostDomainRenderEffect effect,
            Minecraft minecraft
    ) {

        /*
         * ========================================================
         * 判断当前效果是否需要渲染
         * ========================================================
         */

        if (!effect.shouldRender(
                minecraft
        )) {
            return;
        }

        /*
         * ========================================================
         * 获取 Shader
         * ========================================================
         */

        var shaderId =
                effect.shaderId();

        if (!GhostDomainShaderRegistry.isRegistered(
                shaderId
        )) {
            return;
        }

        ShaderInstance shader =
                GhostDomainShaderRegistry.get(
                        shaderId
                );

        /*
         * ========================================================
         * 配置 Shader
         * ========================================================
         */

        setupShader(
                shader
        );

        /*
         * ========================================================
         * 设置鬼域专属 Uniform
         * ========================================================
         */

        effect.setupUniforms(
                shader,
                minecraft
        );

        /*
         * ========================================================
         * 绘制全屏 Quad
         * ========================================================
         */

        drawFullscreenQuad();
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
     * 准备鬼域后处理所需要的深度缓冲。
     *
     * <p>
     * 将 MainRenderTarget 当前帧的深度复制到独立的深度 RenderTarget，
     * 避免后处理阶段直接采样 MainRenderTarget 自身的 Depth Attachment。
     * </p>
     */
    public static void prepareDepth() {

        Minecraft minecraft =
                Minecraft.getInstance();

        var mainTarget =
                minecraft.getMainRenderTarget();

        var depthTarget =
                GhostDomainDepthTarget.get();

        /*
         * ========================================================
         * 复制深度缓冲
         * ========================================================
         *
         * 千万不要直接采样 MainRenderTarget 的 Depth Attachment。
         *
         * 这里有一个非常隐蔽的坑：
         *
         * - 小窗口
         * - 飞行
         * - 特定视角
         *
         * 会出现：
         *
         * - 左上到右下斜边撕裂
         * - 顶部原版残块
         * - 鬼域区域闪烁
         *
         * 根因是：
         *
         * 当前 Framebuffer 一边写 Color，
         * 一边采样自己的 Depth，
         * 触发了 Framebuffer Feedback。
         *
         * 解决办法：
         *
         * 先复制深度到独立 TextureTarget，
         * 再从这个独立深度纹理采样。
         *
         * 注意：
         *
         * copyDepthFrom() 会修改当前 FBO，
         * 所以复制完成后必须重新绑定 MainRenderTarget，
         * 同时恢复 viewport。
         */
        depthTarget.copyDepthFrom(
                mainTarget
        );

        /*
         * 重新绑定主渲染目标。
         */
        mainTarget.bindWrite(false);

        /*
         * 恢复主渲染目标的 viewport。
         */
        RenderSystem.viewport(
                0,
                0,
                mainTarget.width,
                mainTarget.height
        );
    }



    /**
     * 准备指定 Shader 的后处理输入。
     *
     * <p>
     * DiffuseSampler 使用 MainRenderTarget 当前颜色纹理。
     * MainDepthSampler 使用独立复制出来的鬼域深度纹理。
     * </p>
     */
    public static void setupShader(
            ShaderInstance shader
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        var mainTarget =
                minecraft.getMainRenderTarget();

        /*
         * 设置当前后处理 Shader。
         */
        RenderSystem.setShader(
                () -> shader
        );

        /*
         * Minecraft 当前帧的颜色结果。
         */
        shader.setSampler(
                "DiffuseSampler",
                mainTarget.getColorTextureId()
        );

        /*
         * 安全的深度副本。
         *
         * 绝对不能直接使用 mainTarget 的 Depth Attachment，
         * 否则会再次触发 Framebuffer Feedback。
         */
        shader.setSampler(
                "MainDepthSampler",
                GhostDomainDepthTarget.get()
                        .getDepthTextureId()
        );
    }
}