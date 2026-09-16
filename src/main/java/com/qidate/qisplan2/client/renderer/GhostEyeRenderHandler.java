package com.qidate.qisplan2.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.domain.ClientGhostDomain;
import com.qidate.qisplan2.client.domain.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.ability.ghosteye.GhostEyeAbility;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

public final class GhostEyeRenderHandler {

    private GhostEyeRenderHandler() {
    }

    private static Matrix4f capturedModelViewMatrix =
            new Matrix4f();

    public static void captureModelViewMatrix(
            Matrix4f matrix
    ) {
        capturedModelViewMatrix.set(matrix);
    }

    public static Matrix4f getCapturedModelViewMatrix() {
        return capturedModelViewMatrix;
    }

    private static Matrix4f capturedProjectionMatrix =
            new Matrix4f();

    public static void captureProjectionMatrix(
            Matrix4f matrix
    ) {
        capturedProjectionMatrix.set(matrix);
    }

    public static Matrix4f getCapturedProjectionMatrix() {
        return capturedProjectionMatrix;
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

        var mainTarget = minecraft.getMainRenderTarget();

        var depthTarget = GhostEyeDepthTarget.get();

        depthTarget.copyDepthFrom(mainTarget);

        mainTarget.bindWrite(false);

        RenderSystem.viewport(
                0,
                0,
                mainTarget.width,
                mainTarget.height
        );

        ShaderInstance shader = GhostEyeShader.getInstance();

        RenderSystem.setShader(() -> shader);

        int colorTexture =
                minecraft.getMainRenderTarget()
                        .getColorTextureId();

        shader.setSampler(
                "DiffuseSampler",
                colorTexture
        );

        shader.setSampler(
                "MainDepthSampler",
                depthTarget.getDepthTextureId()
        );

        shader.getUniform("GhostEyeProjMat").set(
                RenderSystem.getProjectionMatrix()
        );

        shader.getUniform("GhostEyeModelViewMat").set(
                GhostEyeRenderHandler.getCapturedModelViewMatrix()
        );

        Camera camera =
                minecraft.gameRenderer.getMainCamera();

        shader.getUniform("GhostEyeCameraPos").set(
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

            shader.getUniform(
                    "GhostEyeDomainLayer"
            ).set(
                    (float) ghostEyeDomain.getLayer()
            );

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

            shader.getUniform(
                    "GhostEyeDomainLayer"
            ).set(1.0F);
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
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
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