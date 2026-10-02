package com.qidate.qisplan2.ghost.isolation.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainMatrices;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainShaderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

/**
 * 灵异隔绝 Mask 渲染器。
 *
 * <p>
 * 负责将当前画面的深度信息转换成世界坐标，
 * 并判断像素是否处于灵异隔绝 Cuboid 中。
 * </p>
 *
 * <p>
 * 当前阶段只验证单个 Cuboid 的完整渲染链路。
 * </p>
 */
public final class GhostIsolationMaskRenderer {

    private static final ResourceLocation SHADER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_isolation_mask"
            );

    private GhostIsolationMaskRenderer() {
    }

    /**
     * 渲染灵异隔绝 Mask。
     *
     * <p>
     * 当前版本暂时使用固定 Cuboid，
     * 后续再接 ClientGhostIsolationManager。
     * </p>
     */
    public static void render() {

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

        /*
         * 先获取当前画面的深度。
         */
        var mainTarget =
                minecraft.getMainRenderTarget();

        var maskTarget =
                GhostIsolationMaskTarget.get();

        /*
         * Mask 本身不需要读取颜色，
         * 只需要读取主画面的深度。
         */
        RenderSystem.setShader(
                () -> shader
        );

        shader.setSampler(
                "MainDepthSampler",
                mainTarget.getDepthTextureId()
        );

        /*
         * 使用当前 Minecraft 的投影矩阵。
         */
        shader.getUniform("IsolationProjMat").set(
                RenderSystem.getProjectionMatrix()
        );

        /*
         * 使用当前帧捕获的 ModelView 矩阵。
         *
         * 这与 GhostEye 当前使用的矩阵保持一致。
         */
        shader.getUniform("IsolationModelViewMat").set(
                GhostDomainMatrices.getModelViewMatrix()
        );

        /*
         * Camera 位置。
         */
        var camera =
                minecraft.gameRenderer.getMainCamera();

        shader.getUniform("IsolationCameraPos").set(
                (float) camera.getPosition().x,
                (float) camera.getPosition().y,
                (float) camera.getPosition().z
        );

        /*
         * --------------------------------------------------
         * 当前阶段测试用 Cuboid
         * --------------------------------------------------
         *
         * 先放在玩家附近。
         *
         * 后面这里会彻底替换成：
         *
         * ClientGhostIsolationManager
         *        ↓
         * 当前维度全部 Cuboid
         */
        shader.getUniform("IsolationCuboidMin").set(
                (float) camera.getPosition().x - 3.0F,
                (float) camera.getPosition().y - 2.0F,
                (float) camera.getPosition().z - 3.0F
        );

        shader.getUniform("IsolationCuboidMax").set(
                (float) camera.getPosition().x + 3.0F,
                (float) camera.getPosition().y + 2.0F,
                (float) camera.getPosition().z + 3.0F
        );

        shader.getUniform("IsolationCuboidActive")
                .set(1.0F);

        /*
         * 输出到 Mask RenderTarget。
         */
        maskTarget.bindWrite(false);

        RenderSystem.viewport(
                0,
                0,
                maskTarget.width,
                maskTarget.height
        );

        /*
         * 全屏绘制。
         */
        drawFullscreenQuad();

        /*
         * 渲染完成后重新绑定主画面。
         */
        mainTarget.bindWrite(false);

        RenderSystem.viewport(
                0,
                0,
                mainTarget.width,
                mainTarget.height
        );

        /*
         * 恢复默认状态。
         */
        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
    }

    /**
     * 绘制全屏四边形。
     */
    private static void drawFullscreenQuad() {

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

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}