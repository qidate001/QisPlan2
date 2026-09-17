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

        GhostDomainRenderPipeline.prepareDepth();

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
                minecraft.getMainRenderTarget()
                        .getColorTextureId()
        );

        shader.setSampler(
                "MainDepthSampler",
                GhostDomainDepthTarget.get()
                        .getDepthTextureId()
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

        GhostDomainRenderPipeline.drawFullscreenQuad();
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