package com.qidate.qisplan2.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

public final class GhostRainRenderer {

    /*
     * ============================================================
     * 雨场范围
     * ============================================================
     */

    /**
     * 相机周围扫描多少个方块。
     *
     * -16 ~ +16
     *
     * 即 33 × 33。
     */
    private static final int RAIN_RADIUS = 16;


    /*
     * ============================================================
     * 垂直范围
     * ============================================================
     */

    /**
     * 雨柱最高点相对于摄像机的高度。
     */
    private static final double RAIN_TOP = 14.0D;

    /**
     * 雨柱最低点相对于摄像机的高度。
     */
    private static final double RAIN_BOTTOM = -10.0D;


    /**
     * 一根雨线长度。
     */
    private static final double RAIN_MIN_LENGTH = 3.5D;
    private static final double RAIN_MAX_LENGTH = 6.0D;


    /*
     * ============================================================
     * 下落速度
     * ============================================================
     */

    /**
     * 每 tick 下落多少格。
     */
    private static final double RAIN_MIN_SPEED = 0.45D;
    private static final double RAIN_MAX_SPEED = 0.75D;


    /*
     * ============================================================
     * 外观
     * ============================================================
     */

    /**
     * 雨线半宽。
     */
    private static final float RAIN_WIDTH = 0.035F;

    /**
     * 雨线透明度。
     */
    private static final float RAIN_ALPHA = 0.78F;


    /**
     * 轻微风向。
     */
    private static final double WIND_X = 0.08D;
    private static final double WIND_Z = -0.02D;


    /*
     * ============================================================
     * 雨循环
     * ============================================================
     */

    /**
     * 一整轮下落距离。
     *
     * 从顶部落到底部之后重新回到顶部。
     */
    private static final double RAIN_CYCLE =
            RAIN_TOP - RAIN_BOTTOM;


    private GhostRainRenderer() {
    }


    /*
     * ============================================================
     * 公开渲染入口
     * ============================================================
     */

    public static void render(
            RenderLevelStageEvent event,
            List<GhostRainSource> sources
    ) {

        if (sources.isEmpty()) {
            return;
        }

        Camera camera =
                event.getCamera();

        PoseStack poseStack =
                event.getPoseStack();


        /*
         * ========================================================
         * 摄像机位置
         * ========================================================
         */

        double cameraX =
                camera.getPosition().x;

        double cameraY =
                camera.getPosition().y;

        double cameraZ =
                camera.getPosition().z;


        /*
         * ========================================================
         * 时间
         * ========================================================
         *
         * renderTick：
         * 当前渲染 tick。
         *
         * partialTick：
         * 当前 tick 之间的连续插值。
         *
         * 二者结合以后，雨会连续下落。
         */

        int renderTick =
                event.getRenderTick();

        DeltaTracker deltaTracker =
                event.getPartialTick();

        float partialTick =
                deltaTracker.getGameTimeDeltaPartialTick(false);

        double animationTime =
                renderTick + partialTick;


        /*
         * ========================================================
         * 摄像机所在方块
         * ========================================================
         */

        int centerX =
                Mth.floor(cameraX);

        int centerZ =
                Mth.floor(cameraZ);


        /*
         * ========================================================
         * OpenGL 状态
         * ========================================================
         */

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        /*
         * 雨应该参与深度测试。
         *
         * 因此：
         *
         * 房子会挡住雨。
         * 墙壁会挡住雨。
         * 地面会挡住雨。
         *
         * 但我们不写入自己的深度。
         */
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.disableCull();

        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );


        /*
         * ========================================================
         * 顶点缓冲
         * ========================================================
         */

        BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(
                                VertexFormat.Mode.QUADS,
                                DefaultVertexFormat.POSITION_COLOR
                        );


        /*
         * ========================================================
         * 所有鬼雨领域
         * ========================================================
         */

        for (GhostRainSource source : sources) {

            renderSource(
                    buffer,
                    poseStack,
                    source,
                    centerX,
                    centerZ,
                    cameraX,
                    cameraY,
                    cameraZ,
                    animationTime
            );
        }


        /*
         * ========================================================
         * 提交
         * ========================================================
         */

        MeshData mesh =
                buffer.buildOrThrow();

        BufferUploader.drawWithShader(
                mesh
        );


        /*
         * ========================================================
         * 恢复状态
         * ========================================================
         */

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }


    /*
     * ============================================================
     * 渲染一个鬼雨领域
     * ============================================================
     */

    private static void renderSource(
            BufferBuilder buffer,
            PoseStack poseStack,
            GhostRainSource source,
            int centerX,
            int centerZ,
            double cameraX,
            double cameraY,
            double cameraZ,
            double animationTime
    ) {

        /*
         * ========================================================
         * 计算扫描范围
         * ========================================================
         */

        int minX =
                centerX - RAIN_RADIUS;

        int maxX =
                centerX + RAIN_RADIUS;

        int minZ =
                centerZ - RAIN_RADIUS;

        int maxZ =
                centerZ + RAIN_RADIUS;


        /*
         * ========================================================
         * 世界雨列
         * ========================================================
         */

        for (int worldX = minX;
             worldX <= maxX;
             worldX++) {

            for (int worldZ = minZ;
                 worldZ <= maxZ;
                 worldZ++) {

                /*
                 * ------------------------------------------------
                 * 检查这个雨列是否在鬼雨领域里
                 * ------------------------------------------------
                 */

                double domainDX =
                        (worldX + 0.5D)
                                - source.x();

                double domainDZ =
                        (worldZ + 0.5D)
                                - source.z();

                double domainDistanceSqr =
                        domainDX * domainDX
                                + domainDZ * domainDZ;

                double radius =
                        source.radius();

                if (domainDistanceSqr
                        > radius * radius) {

                    continue;
                }


                /*
                 * ------------------------------------------------
                 * 世界坐标稳定随机
                 * ------------------------------------------------
                 */

                long seed =
                        mixRainSeed(
                                worldX,
                                worldZ
                        );


                /*
                 * X 偏移。
                 */
                double offsetX =
                        random01(seed)
                                * 0.85D;


                /*
                 * Z 偏移。
                 */
                double offsetZ =
                        random01(
                                seed
                                        ^ 0x5DEECE66DL
                        ) * 0.85D;


                /*
                 * 速度。
                 */
                double speedRandom =
                        random01(
                                seed
                                        ^ 0x9E3779B97F4A7C15L
                        );


                double speed =
                        Mth.lerp(
                                speedRandom,
                                RAIN_MIN_SPEED,
                                RAIN_MAX_SPEED
                        );


                /*
                 * 长度。
                 */
                double lengthRandom =
                        random01(
                                seed
                                        ^ 0xC2B2AE3D27D4EB4FL
                        );

                double length =
                        Mth.lerp(
                                lengthRandom,
                                RAIN_MIN_LENGTH,
                                RAIN_MAX_LENGTH
                        );


                /*
                 * 初始相位。
                 *
                 * 让不同雨列不要同时落下。
                 */
                double phase =
                        random01(
                                seed
                                        ^ 0x94D049BB133111EBL
                        ) * RAIN_CYCLE;


                /*
                 * ------------------------------------------------
                 * 连续下落
                 * ------------------------------------------------
                 */

                double fall =
                        (animationTime * speed + phase)
                                % RAIN_CYCLE;


                /*
                 * ------------------------------------------------
                 * 雨的世界坐标
                 * ------------------------------------------------
                 */

                double rainWorldX =
                        worldX + offsetX;

                double rainWorldZ =
                        worldZ + offsetZ;


                /*
                 * 顶端。
                 *
                 * RAIN_TOP → RAIN_BOTTOM
                 */
                double rainWorldY =
                        cameraY
                                + RAIN_TOP
                                - fall;


                /*
                 * ------------------------------------------------
                 * 底端
                 * ------------------------------------------------
                 */

                double rainWorldBottomY =
                        rainWorldY - length;


                /*
                 * ------------------------------------------------
                 * 风偏
                 * ------------------------------------------------
                 */

                double rainWorldBottomX =
                        rainWorldX + WIND_X;

                double rainWorldBottomZ =
                        rainWorldZ + WIND_Z;


                /*
                 * ------------------------------------------------
                 * 世界坐标
                 *      ↓
                 * Camera-relative
                 * ------------------------------------------------
                 */

                double topX =
                        rainWorldX
                                - cameraX;

                double topY =
                        rainWorldY
                                - cameraY;

                double topZ =
                        rainWorldZ
                                - cameraZ;


                double bottomX =
                        rainWorldBottomX
                                - cameraX;

                double bottomY =
                        rainWorldBottomY
                                - cameraY;

                double bottomZ =
                        rainWorldBottomZ
                                - cameraZ;


                /*
                 * ------------------------------------------------
                 * 如果雨线完全在摄像机下方，
                 * 可以直接跳过。
                 * ------------------------------------------------
                 */

                if (bottomY < RAIN_BOTTOM - 2.0D) {
                    continue;
                }


                /*
                 * ------------------------------------------------
                 * 绘制
                 * ------------------------------------------------
                 */

                drawRainLine(
                        buffer,
                        poseStack,
                        topX,
                        topY,
                        topZ,
                        bottomX,
                        bottomY,
                        bottomZ,
                        RAIN_WIDTH
                );
            }
        }
    }


    /*
     * ============================================================
     * 绘制一根雨线
     * ============================================================
     */

    private static void drawRainLine(
            BufferBuilder buffer,
            PoseStack poseStack,
            double topX,
            double topY,
            double topZ,
            double bottomX,
            double bottomY,
            double bottomZ,
            float width
    ) {

        var pose =
                poseStack.last().pose();


        /*
         * ========================================================
         * X-Y 平面
         * ========================================================
         */

        buffer.addVertex(
                pose,
                (float) (topX - width),
                (float) topY,
                (float) topZ
        ).setColor(
                0.0F,
                0.0F,
                0.0F,
                RAIN_ALPHA
        );

        buffer.addVertex(
                pose,
                (float) (topX + width),
                (float) topY,
                (float) topZ
        ).setColor(
                0.0F,
                0.0F,
                0.0F,
                RAIN_ALPHA
        );

        buffer.addVertex(
                pose,
                (float) (bottomX + width),
                (float) bottomY,
                (float) bottomZ
        ).setColor(
                0.0F,
                0.0F,
                0.0F,
                RAIN_ALPHA
        );

        buffer.addVertex(
                pose,
                (float) (bottomX - width),
                (float) bottomY,
                (float) bottomZ
        ).setColor(
                0.0F,
                0.0F,
                0.0F,
                RAIN_ALPHA
        );


        /*
         * ========================================================
         * Z-Y 平面
         * ========================================================
         */

        buffer.addVertex(
                pose,
                (float) topX,
                (float) topY,
                (float) (topZ - width)
        ).setColor(
                0.0F,
                0.0F,
                0.0F,
                RAIN_ALPHA
        );

        buffer.addVertex(
                pose,
                (float) topX,
                (float) topY,
                (float) (topZ + width)
        ).setColor(
                0.0F,
                0.0F,
                0.0F,
                RAIN_ALPHA
        );

        buffer.addVertex(
                pose,
                (float) bottomX,
                (float) bottomY,
                (float) (bottomZ + width)
        ).setColor(
                0.0F,
                0.0F,
                0.0F,
                RAIN_ALPHA
        );

        buffer.addVertex(
                pose,
                (float) bottomX,
                (float) bottomY,
                (float) (bottomZ - width)
        ).setColor(
                0.0F,
                0.0F,
                0.0F,
                RAIN_ALPHA
        );
    }


    /*
     * ============================================================
     * 世界坐标稳定随机
     * ============================================================
     */

    private static long mixRainSeed(
            long x,
            long z
    ) {

        long seed =
                x * 341873128712L
                        + z * 132897987541L;

        seed ^=
                seed >>> 13;

        seed *=
                1274126177L;

        seed ^=
                seed >>> 16;

        return seed;
    }


    /*
     * ============================================================
     * 0 ~ 1 稳定随机
     * ============================================================
     */

    private static double random01(
            long value
    ) {

        value ^=
                value >>> 33;

        value *=
                0xff51afd7ed558ccdL;

        value ^=
                value >>> 33;

        value *=
                0xc4ceb9fe1a85ec53L;

        value ^=
                value >>> 33;

        return (
                value & Long.MAX_VALUE
        ) / (double) Long.MAX_VALUE;
    }
}