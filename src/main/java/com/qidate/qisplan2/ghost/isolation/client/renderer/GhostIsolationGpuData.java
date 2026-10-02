package com.qidate.qisplan2.ghost.isolation.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationCuboid;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationManager;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationRegion;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

/**
 * 灵异隔绝 Cuboid 的 GPU 数据。
 *
 * <p>
 * 使用一张 RGBA32F 纹理保存所有 Cuboid 的空间数据。
 *
 * <pre>
 * texture width  = 2
 * texture height = MAX_CUBOIDS
 *
 * x = 0 → Min
 * x = 1 → Max
 *
 * y = Cuboid index
 * </pre>
 *
 * <p>
 * 每个 Texel：
 *
 * <pre>
 * R = X
 * G = Y
 * B = Z
 * A = 1
 * </pre>
 */
public final class GhostIsolationGpuData {

    /**
     * GPU 最多保存的 Cuboid 数量。
     */
    public static final int MAX_CUBOIDS = 256;

    /**
     * 数据纹理宽度。
     *
     * <p>
     * 0 = Min
     * 1 = Max
     */
    private static final int TEXTURE_WIDTH = 2;

    /**
     * OpenGL 数据纹理。
     *
     * <p>
     * 只能在 Render Thread 创建和操作。
     */
    private static int textureId = 0;

    /**
     * 当前 Cuboid 数量。
     *
     * <p>
     * 这个值属于客户端逻辑数据，
     * 不依赖 GPU Texture。
     */
    private static int cuboidCount = 0;

    /**
     * CPU 侧 Min 数据。
     *
     * <p>
     * 每三个 float 表示一个 Cuboid：
     *
     * <pre>
     * [x, y, z]
     * </pre>
     */
    private static final float[] CUBOID_MINS =
            new float[MAX_CUBOIDS * 3];

    /**
     * CPU 侧 Max 数据。
     *
     * <p>
     * 注意：
     *
     * <pre>
     * max = Minecraft max + 1
     * </pre>
     *
     * 这样 GPU 中保存的是连续空间的半开区间。
     */
    private static final float[] CUBOID_MAXS =
            new float[MAX_CUBOIDS * 3];

    /**
     * GPU 是否需要重新上传。
     */
    private static boolean dirty = true;

    private GhostIsolationGpuData() {
    }

    /**
     * 根据客户端当前 Region，
     * 重建 CPU 侧 Cuboid 数据。
     *
     * <p>
     * 注意：
     *
     * <strong>
     * 这个方法只处理 CPU 数据，
     * 不进行任何 OpenGL 操作。
     * </strong>
     *
     * <p>
     * 因此可以安全地在
     * Client 网络工作线程中调用。
     */
    public static void rebuild() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            clear();
            return;
        }

        clearCpuData();

        int count = 0;

        /*
         * ====================================================
         * 遍历当前客户端所有 Region
         * ====================================================
         */
        for (ClientGhostIsolationRegion region :
                ClientGhostIsolationManager.getRegions()) {

            /*
             * 只处理当前维度。
             */
            if (!region.getDimension().equals(
                    minecraft.level.dimension().location()
            )) {
                continue;
            }

            /*
             * =================================================
             * 展开 Region 中的全部 Cuboid
             * =================================================
             */
            for (ClientGhostIsolationCuboid cuboid :
                    region.getCuboids()) {

                if (count >= MAX_CUBOIDS) {

                    QisPlan2.LOGGER.warn(
                            "[灵异隔绝 GPU] Cuboid 数量超过上限 {}，"
                                    + "后续 Cuboid 将被忽略。",
                            MAX_CUBOIDS
                    );

                    cuboidCount =
                            count;

                    dirty =
                            true;

                    return;
                }

                int offset =
                        count * 3;

                /*
                 * =================================================
                 * Min
                 * =================================================
                 */
                CUBOID_MINS[offset] =
                        cuboid.minX();

                CUBOID_MINS[offset + 1] =
                        cuboid.minY();

                CUBOID_MINS[offset + 2] =
                        cuboid.minZ();

                /*
                 * =================================================
                 * Max
                 *
                 * Minecraft 的 Cuboid max 是包含边界。
                 *
                 * Shader 使用：
                 *
                 * min <= position < max
                 *
                 * 因此这里必须 +1。
                 * =================================================
                 */
                CUBOID_MAXS[offset] =
                        cuboid.maxX() + 1.0F;

                CUBOID_MAXS[offset + 1] =
                        cuboid.maxY() + 1.0F;

                CUBOID_MAXS[offset + 2] =
                        cuboid.maxZ() + 1.0F;

                count++;
            }
        }

        cuboidCount =
                count;

        /*
         * CPU 数据发生变化。
         *
         * GPU 数据暂时不动。
         *
         * 等 Render Thread 调用
         * uploadIfNeeded() 时再上传。
         */
        dirty =
                true;

        QisPlan2.LOGGER.info(
                "[灵异隔绝 GPU] 重建数据：Cuboid={}",
                cuboidCount
        );
    }

    /**
     * 在 Render Thread 中上传当前数据。
     *
     * <p>
     * 这是唯一负责 OpenGL Texture 上传的方法。
     */
    public static void uploadIfNeeded() {

        /*
         * 防止错误线程调用。
         */
        RenderSystem.assertOnRenderThreadOrInit();

        if (!dirty) {
            return;
        }

        ensureTexture();

        /*
         * ====================================================
         * 创建 FloatBuffer
         *
         * 每个 Cuboid：
         *
         * Min = 4 float
         * Max = 4 float
         *
         * 总大小：
         *
         * 2 × MAX_CUBOIDS × 4
         * ====================================================
         */
        FloatBuffer buffer =
                BufferUtils.createFloatBuffer(
                        TEXTURE_WIDTH
                                * MAX_CUBOIDS
                                * 4
                );

        for (int i = 0; i < MAX_CUBOIDS; i++) {

            int offset =
                    i * 3;

            /*
             * =================================================
             * x = 0：Min
             * =================================================
             */
            buffer.put(
                    CUBOID_MINS[offset]
            );

            buffer.put(
                    CUBOID_MINS[offset + 1]
            );

            buffer.put(
                    CUBOID_MINS[offset + 2]
            );

            buffer.put(
                    1.0F
            );

            /*
             * =================================================
             * x = 1：Max
             * =================================================
             */
            buffer.put(
                    CUBOID_MAXS[offset]
            );

            buffer.put(
                    CUBOID_MAXS[offset + 1]
            );

            buffer.put(
                    CUBOID_MAXS[offset + 2]
            );

            buffer.put(
                    1.0F
            );
        }

        buffer.flip();

        /*
         * ====================================================
         * 绑定 Texture
         * ====================================================
         */
        RenderSystem.bindTexture(
                textureId
        );

        /*
         * ====================================================
         * 上传 RGBA32F Texture
         * ====================================================
         *
         * 这里不能使用 Minecraft 的
         * GlStateManager._texImage2D，
         * 因为你这个 1.21.1 映射的参数是 IntBuffer。
         *
         * 直接调用 LWJGL 的 GL11.glTexImage2D，
         * 可以使用 FloatBuffer。
         * ====================================================
         */
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL30.GL_RGBA32F,
                TEXTURE_WIDTH,
                MAX_CUBOIDS,
                0,
                GL11.GL_RGBA,
                GL11.GL_FLOAT,
                buffer
        );

        /*
         * ====================================================
         * Texture 参数
         * ====================================================
         *
         * Cuboid 数据绝对不能线性插值。
         */
        GlStateManager._texParameter(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MIN_FILTER,
                GL11.GL_NEAREST
        );

        GlStateManager._texParameter(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MAG_FILTER,
                GL11.GL_NEAREST
        );

        /*
         * 不需要重复。
         */
        GlStateManager._texParameter(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_S,
                GL12.GL_CLAMP_TO_EDGE
        );

        GlStateManager._texParameter(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_T,
                GL12.GL_CLAMP_TO_EDGE
        );

        /*
         * GPU 数据现在与 CPU 数据同步。
         */
        dirty =
                false;
    }

    /**
     * 创建 GPU Texture。
     *
     * <p>
     * 只能在 Render Thread 中调用。
     */
    private static void ensureTexture() {

        if (textureId != 0) {
            return;
        }

        textureId =
                GlStateManager._genTexture();

        QisPlan2.LOGGER.info(
                "[灵异隔绝 GPU] 创建数据纹理：id={}",
                textureId
        );
    }

    /**
     * 获取 GPU Texture ID。
     *
     * <p>
     * 调用者必须确保此前已经执行：
     *
     * <pre>
     * uploadIfNeeded()
     * </pre>
     */
    public static int getTextureId() {
        return textureId;
    }

    /**
     * 获取当前 Cuboid 数量。
     */
    public static int getCuboidCount() {
        return cuboidCount;
    }

    /**
     * 清空客户端 CPU 数据。
     *
     * <p>
     * 不进行 OpenGL 操作。
     */
    public static void clear() {

        clearCpuData();

        cuboidCount =
                0;

        dirty =
                true;

        QisPlan2.LOGGER.info(
                "[灵异隔绝 GPU] 数据已清空"
        );
    }

    /**
     * 清空 CPU 数据。
     */
    private static void clearCpuData() {

        java.util.Arrays.fill(
                CUBOID_MINS,
                0.0F
        );

        java.util.Arrays.fill(
                CUBOID_MAXS,
                0.0F
        );
    }

    /**
     * 销毁 GPU Texture。
     *
     * <p>
     * 必须在 Render Thread 调用。
     */
    public static void destroy() {

        RenderSystem.assertOnRenderThreadOrInit();

        if (textureId != 0) {

            GlStateManager._deleteTexture(
                    textureId
            );

            textureId =
                    0;
        }

        cuboidCount =
                0;

        dirty =
                true;

        clearCpuData();

        QisPlan2.LOGGER.info(
                "[灵异隔绝 GPU] 数据纹理已销毁"
        );
    }
}