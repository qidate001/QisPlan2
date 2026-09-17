package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomain;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.ability.ghosteye.GhostEyeAbility;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

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

        if (!GhostDomainShaderRegistry.isRegistered(
                GhostEyeAbility.ID
        )) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        /*
         * ========================================================
         * 当前鬼眼鬼域
         * ========================================================
         */

        ClientGhostDomain ghostEyeDomain =
                getCurrentGhostEyeDomain(
                        minecraft
                );

        /*
         * 没有鬼眼开启时，不需要复制深度，
         * 也不需要执行任何后处理。
         */
        if (ghostEyeDomain == null) {
            return;
        }

        /*
         * ========================================================
         * 准备 RenderTarget
         * ========================================================
         */

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

        mainTarget.bindWrite(false);

        RenderSystem.viewport(
                0,
                0,
                mainTarget.width,
                mainTarget.height
        );

        /*
         * ========================================================
         * 配置 Shader
         * ========================================================
         */

        ShaderInstance shader =
                GhostDomainShaderRegistry.get(
                        GhostEyeAbility.ID
                );

        RenderSystem.setShader(
                () -> shader
        );

        shader.setSampler(
                "DiffuseSampler",
                mainTarget.getColorTextureId()
        );

        shader.setSampler(
                "MainDepthSampler",
                depthTarget.getDepthTextureId()
        );

        shader.getUniform(
                "GhostEyeProjMat"
        ).set(
                RenderSystem.getProjectionMatrix()
        );

        shader.getUniform(
                "GhostEyeModelViewMat"
        ).set(
                GhostDomainMatrices.getModelViewMatrix()
        );

        Camera camera =
                minecraft.gameRenderer
                        .getMainCamera();

        shader.getUniform(
                "GhostEyeCameraPos"
        ).set(
                (float) camera.getPosition().x,
                (float) camera.getPosition().y,
                (float) camera.getPosition().z
        );

        shader.getUniform(
                "GhostEyeDomainCenter"
        ).set(
                (float) ghostEyeDomain.getX(),
                (float) ghostEyeDomain.getY(),
                (float) ghostEyeDomain.getZ()
        );

        shader.getUniform(
                "GhostEyeDomainRadius"
        ).set(
                (float) ghostEyeDomain.getRadius()
        );

        shader.getUniform(
                "GhostEyeDomainActive"
        ).set(1.0F);

        shader.getUniform(
                "GhostEyeDomainLayer"
        ).set(
                (float) ghostEyeDomain.getLayer()
        );

        /*
         * ========================================================
         * 绘制全屏 Quad
         * ========================================================
         */

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

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
         * ========================================================
         * 恢复渲染状态
         * ========================================================
         *
         * 后面还有第一人称手、GUI 等渲染，
         * 不恢复状态容易留下连锁 Bug。
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

    private static ClientGhostDomain getCurrentGhostEyeDomain(
            Minecraft minecraft
    ) {

        if (minecraft.level == null) {
            return null;
        }

        return ClientGhostDomainManager.getFirstDomainByType(
                GhostEyeAbility.ID
        );
    }
}