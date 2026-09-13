package com.qidate.qisplan2.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.qidate.qisplan2.client.domain.ClientGhostDomain;
import com.qidate.qisplan2.client.domain.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.ability.ghosteye.GhostEyeAbility;
import net.minecraft.client.Camera;
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

        shader.getUniform("ModelViewMat").set(
                RenderSystem.getModelViewMatrix()
        );

        Camera camera =
                minecraft.gameRenderer.getMainCamera();

        shader.getUniform("CameraPos").set(
                (float) camera.getPosition().x,
                (float) camera.getPosition().y,
                (float) camera.getPosition().z
        );

        ClientGhostDomain ghostEyeDomain =
                getCurrentGhostEyeDomain(minecraft);

        if (ghostEyeDomain != null) {

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

        } else {

            shader.getUniform(
                    "GhostEyeDomainCenter"
            ).set(
                    0.0F,
                    0.0F,
                    0.0F
            );

            shader.getUniform(
                    "GhostEyeDomainRadius"
            ).set(0.0F);

            shader.getUniform(
                    "GhostEyeDomainActive"
            ).set(0.0F);
        }

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

    private static ClientGhostDomain getCurrentGhostEyeDomain(
            Minecraft minecraft
    ) {
        if (minecraft.level == null) {
            return null;
        }

        Camera camera =
                minecraft.gameRenderer.getMainCamera();

        ClientGhostDomain domain =
                ClientGhostDomainManager.getEffectiveDomain(
                        camera.getPosition().x,
                        camera.getPosition().y,
                        camera.getPosition().z
                );

        if (domain == null) {
            return null;
        }

        if (!GhostEyeAbility.ID.equals(domain.getDomainType())) {
            return null;
        }

        return domain;
    }
}