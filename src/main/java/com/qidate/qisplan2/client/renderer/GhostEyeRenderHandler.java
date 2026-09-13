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

    private static int debugLogTicks = 0;

    private static void debugLog() {

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Camera camera =
                minecraft.gameRenderer.getMainCamera();

        ClientGhostDomain domain =
                getCurrentGhostEyeDomain(minecraft);

        if (domain == null) {
            QisPlan2.LOGGER.info(
                    "[GhostEye DEBUG] domain=null camera=({}, {}, {})",
                    camera.getPosition().x,
                    camera.getPosition().y,
                    camera.getPosition().z
            );
            return;
        }

        double dx =
                domain.getX() - camera.getPosition().x;

        double dy =
                domain.getY() - camera.getPosition().y;

        double dz =
                domain.getZ() - camera.getPosition().z;

        QisPlan2.LOGGER.info(
                "[GhostEye DEBUG] " +
                        "camera=({}, {}, {}) " +
                        "domain=({}, {}, {}) " +
                        "delta=({}, {}, {}) " +
                        "radius={} layer={}",

                camera.getPosition().x,
                camera.getPosition().y,
                camera.getPosition().z,

                domain.getX(),
                domain.getY(),
                domain.getZ(),

                dx,
                dy,
                dz,

                domain.getRadius(),
                domain.getLayer()
        );

        Matrix4f matrix =
                getCapturedModelViewMatrix();

        QisPlan2.LOGGER.info(
                "[GhostEye DEBUG] ModelViewMatrix={}",
                matrix
        );
    }

    /**
     * 当前阶段仅用于测试：
     *
     * 世界渲染完成后绘制一个覆盖整个屏幕的 Quad。
     * Shader 本身会将它渲染成纯红色。
     */
    public static void render() {

        debugLogTicks++;

        if (debugLogTicks >= 20) {
            debugLogTicks = 0;

            debugLog();
        }

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
                GhostEyeRenderHandler.getCapturedModelViewMatrix()
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