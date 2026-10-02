package com.qidate.qisplan2.ghost.isolation.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainDepthTarget;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainMatrices;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainRenderPipeline;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainShaderRegistry;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationCuboid;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationManager;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

/**
 * 灵异隔绝 Mask 渲染器。
 *
 * <p>
 * 负责根据当前画面的深度信息，
 * 判断屏幕像素对应的世界位置是否处于
 * 灵异隔绝 Cuboid 中。
 * </p>
 *
 * <p>
 * 当前阶段仅用于验证单个测试 Cuboid。
 * </p>
 */
public final class GhostIsolationMaskRenderer {

    private static final ResourceLocation SHADER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_isolation_mask"
            );

    private static final boolean DEBUG_OUTPUT_TO_SCREEN = true;

    private static String LAST_DEBUG_CUBOID = "";

    private GhostIsolationMaskRenderer() {
    }

    /**
     * 渲染当前帧的灵异隔绝 Mask。
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
         * ====================================================
         * 1. 先复制主画面的 Depth
         * ====================================================
         *
         * 后面的 Mask Shader 绝对不能直接采样
         * MainRenderTarget 当前正在使用的 Depth Attachment。
         *
         * GhostDomainDepthTarget 是独立的 Depth RenderTarget。
         */
        var mainTarget =
                minecraft.getMainRenderTarget();

        var depthTarget =
                GhostDomainDepthTarget.get();

        var outputTarget =
                DEBUG_OUTPUT_TO_SCREEN
                        ? minecraft.getMainRenderTarget()
                        : GhostIsolationMaskTarget.get();

        /*
         * ====================================================
         * 2. 设置 Shader
         * ====================================================
         */
        RenderSystem.setShader(
                () -> shader
        );

        /*
         * 只读取已经复制出来的 Depth。
         */
        shader.setSampler(
                "MainDepthSampler",
                depthTarget.getDepthTextureId()
        );

        /*
         * 当前投影矩阵。
         */
        shader.getUniform("IsolationProjMat").set(
                RenderSystem.getProjectionMatrix()
        );

        /*
         * 当前 ModelView 矩阵。
         *
         * 与 GhostEye 当前使用的矩阵保持一致。
         */
        shader.getUniform("IsolationModelViewMat").set(
                GhostDomainMatrices.getModelViewMatrix()
        );

        /*
         * 当前 Camera 世界坐标。
         */
        var camera =
                minecraft.gameRenderer.getMainCamera();

        shader.getUniform("IsolationCameraPos").set(
                (float) camera.getPosition().x,
                (float) camera.getPosition().y,
                (float) camera.getPosition().z
        );

        /*
         * ====================================================
         * 3. 获取当前维度的第一个真实隔绝 Cuboid
         * ====================================================
         *
         * 当前阶段只测试一个 Cuboid。
         *
         * 后续再扩展成：
         *
         * Region
         *   ↓
         * Cuboids
         *   ↓
         * 多 Cuboid GPU 数据
         */
        ClientGhostIsolationCuboid cuboid = null;

        for (ClientGhostIsolationRegion region :
                ClientGhostIsolationManager.getRegions()) {

            if (!region.getDimension().equals(
                    minecraft.level.dimension().location()
            )) {
                continue;
            }

            for (ClientGhostIsolationCuboid candidate :
                    region.getCuboids()) {

                if (candidate.contains(
                        (int) camera.getPosition().x,
                        (int) camera.getPosition().y,
                        (int) camera.getPosition().z
                )) {
                    cuboid = candidate;
                    break;
                }
            }

            if (cuboid != null) {
                break;
            }
        }

        String debugCuboid;

        if (cuboid == null) {
            debugCuboid = "NONE";
        } else {
            debugCuboid =
                    cuboid.minX() + "," +
                            cuboid.minY() + "," +
                            cuboid.minZ() + " -> " +
                            cuboid.maxX() + "," +
                            cuboid.maxY() + "," +
                            cuboid.maxZ();
        }

        if (!debugCuboid.equals(LAST_DEBUG_CUBOID)) {

            LAST_DEBUG_CUBOID = debugCuboid;

            QisPlan2.LOGGER.info(
                    "[灵异隔绝 Mask] Cuboid = {}",
                    debugCuboid
            );
        }

        /*
         * 默认关闭 Cuboid。
         *
         * 没有客户端同步到任何隔绝区域时，
         * Shader 应该输出纯黑。
         */
        if (cuboid == null) {

            /*
             * 当前摄像机不在任何灵异隔绝 Cuboid 内。
             *
             * 关闭当前 Cuboid。
             */
            shader.getUniform("IsolationCuboidActive").set(0.0F);

        } else {

            shader.getUniform("IsolationCuboidMin").set(
                    (float) cuboid.minX(),
                    (float) cuboid.minY(),
                    (float) cuboid.minZ()
            );

            shader.getUniform("IsolationCuboidMax").set(
                    (float) cuboid.maxX(),
                    (float) cuboid.maxY(),
                    (float) cuboid.maxZ()
            );

            shader.getUniform("IsolationCuboidActive").set(1.0F);
        }

        /*
         * ====================================================
         * 4. 输出到 Mask RenderTarget
         * ====================================================
         */
        outputTarget.bindWrite(false);

        RenderSystem.viewport(
                0,
                0,
                outputTarget.width,
                outputTarget.height
        );

        /*
         * 清空上一帧 Mask。
         *
         * 黑色 = 非隔绝。
         */
        if (!DEBUG_OUTPUT_TO_SCREEN) {
            outputTarget.clear(
                    Minecraft.ON_OSX
            );
        }

        /*
         * Mask Pass 不需要深度测试。
         */
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        drawFullscreenQuad();

        /*
         * 恢复状态。
         */
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        /*
         * 重新绑定主画面。
         *
         * 注意：
         * 这里只是恢复写入目标，
         * 并没有把 Mask 写进 MainRenderTarget。
         */
        mainTarget.bindWrite(false);

        RenderSystem.viewport(
                0,
                0,
                mainTarget.width,
                mainTarget.height
        );

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
    }
}