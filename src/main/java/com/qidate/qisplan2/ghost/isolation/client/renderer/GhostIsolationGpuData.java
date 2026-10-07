package com.qidate.qisplan2.ghost.isolation.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationCuboid;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationManager;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.util.Arrays;

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
 * A = Region Index / 其他数据
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
     * 每个 Cuboid 所属的隔绝空间 Region Index。
     *
     * <p>
     * 下标与 CUBOID_MINS / CUBOID_MAXS 中的 Cuboid 下标一致。
     */
    private static final int[] CUBOID_REGION_INDICES =
            new int[MAX_CUBOIDS];

    /**
     * GPU 是否需要重新上传。
     */
    private static boolean dirty = true;

    /**
     * OpenGL Texture 上传用的持久化 Native Buffer。
     *
     * <p>
     * 生命周期与本类一致。
     *
     * <p>
     * 只能在 Render Thread 使用。
     */
    private static FloatBuffer uploadBuffer;

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

        cuboidCount = 0;

        Arrays.fill(
                CUBOID_REGION_INDICES,
                -1
        );

        /*
         * 当前客户端没有世界时，
         * GPU 数据直接清空。
         */
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            dirty = true;
            return;
        }

        ResourceLocation currentDimension =
                minecraft.level
                        .dimension()
                        .location();

        /*
         * 遍历当前客户端已知的所有隔绝空间。
         */
        for (ClientGhostIsolationRegion region :
                ClientGhostIsolationManager.getRegions()) {

            /*
             * GPU 数据只上传当前维度的隔绝空间。
             */
            if (!region.getDimension().equals(
                    currentDimension
            )) {
                continue;
            }

            /*
             * 获取该 Region 在当前 GPU 数据中的临时 Index。
             */
            int regionIndex =
                    ClientGhostIsolationManager.getRegionIndex(
                            region.getId()
                    );

            /*
             * 理论上不应该发生。
             */
            if (regionIndex < 0) {
                continue;
            }

            /*
             * 一个 Region 可以包含多个 Cuboid。
             */
            for (ClientGhostIsolationCuboid cuboid :
                    region.getCuboids()) {

                /*
                 * ====================================================
                 * 客户端 Chunk 加载状态过滤
                 * ====================================================
                 *
                 * 如果这个 Cuboid 所覆盖的所有 Chunk
                 * 都已经离开客户端的 Chunk Cache，
                 * 那么客户端当前根本没有对应的世界几何。
                 *
                 * 此时不要把这个 Cuboid 提交给 GPU。
                 *
                 * 这样可以避免：
                 *
                 *     世界墙体 Chunk 已卸载
                 *             ↓
                 *     MainDepth 中没有墙
                 *             ↓
                 *     ghost_isolation_region.fsh
                 *     射线穿过本应存在的墙体
                 *             ↓
                 *     错误识别为隔绝空间
                 *
                 * 注意：
                 *
                 * 这里只过滤“完全没有已加载 Chunk”的 Cuboid。
                 *
                 * 如果 Cuboid 横跨：
                 *
                 *     已加载 Chunk + 未加载 Chunk
                 *
                 * 第一版仍然保留整个 Cuboid。
                 *
                 * 后续如果需要更精确的处理，
                 * 再进行 Cuboid → Chunk 裁剪。
                 * ====================================================
                 */
                if (!isCuboidInLoadedChunk(
                        minecraft,
                        cuboid
                )) {
                    continue;
                }

                if (cuboidCount >= MAX_CUBOIDS) {
                    break;
                }

                int base =
                        cuboidCount * 3;

                /*
                 * Cuboid 最小坐标。
                 */
                CUBOID_MINS[base] =
                        cuboid.minX();

                CUBOID_MINS[base + 1] =
                        cuboid.minY();

                CUBOID_MINS[base + 2] =
                        cuboid.minZ();

                /*
                 * Cuboid 最大坐标。
                 *
                 * Shader 使用连续空间 [min, max)。
                 * 因此 Minecraft 的 inclusive max
                 * 需要转换成 max + 1。
                 */
                CUBOID_MAXS[base] =
                        cuboid.maxX() + 1.0F;

                CUBOID_MAXS[base + 1] =
                        cuboid.maxY() + 1.0F;

                CUBOID_MAXS[base + 2] =
                        cuboid.maxZ() + 1.0F;

                /*
                 * 记录这个 Cuboid 属于哪个 Region。
                 */
                CUBOID_REGION_INDICES[cuboidCount] =
                        regionIndex;

                cuboidCount++;
            }
        }

        /*
         * 标记 GPU 数据需要重新上传。
         */
        dirty = true;
    }

    /**
     * 判断指定 Cuboid 是否至少与一个当前客户端
     * 已加载的 Chunk 相交。
     *
     * <p>
     * 这里只查询 ClientChunkCache。
     * 不会请求加载 Chunk。
     *
     * <p>
     * {@code requireChunk = false} 是关键：
     *
     * <pre>
     * 已加载   → 返回 LevelChunk
     * 未加载   → 返回 null
     * </pre>
     */
    private static boolean isCuboidInLoadedChunk(
            Minecraft minecraft,
            ClientGhostIsolationCuboid cuboid
    ) {

        /*
         * Minecraft 方块坐标转换为 Chunk 坐标。
         *
         * 使用 >> 4 可以正确处理负坐标，
         * 因为 Java 对负整数的右移等价于向负无穷方向
         * 的 2^4 整除结果。
         */
        int minChunkX =
                cuboid.minX() >> 4;

        int maxChunkX =
                cuboid.maxX() >> 4;

        int minChunkZ =
                cuboid.minZ() >> 4;

        int maxChunkZ =
                cuboid.maxZ() >> 4;

        /*
         * 获取当前客户端 Chunk Cache。
         */
        var chunkSource =
                minecraft.level.getChunkSource();

        /*
         * 检查 Cuboid 覆盖范围内的所有 Chunk。
         *
         * 只要找到一个已经加载的 Chunk，
         * 就认为这个 Cuboid 当前仍然应该提交 GPU。
         */
        for (int chunkX = minChunkX;
             chunkX <= maxChunkX;
             chunkX++) {

            for (int chunkZ = minChunkZ;
                 chunkZ <= maxChunkZ;
                 chunkZ++) {

                if (chunkSource.getChunk(
                        chunkX,
                        chunkZ,
                        ChunkStatus.FULL,
                        false
                ) != null) {

                    return true;
                }
            }
        }

        /*
         * 整个 Cuboid 都位于当前客户端
         * 尚未加载的 Chunk 中。
         */
        return false;
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
        ensureUploadBuffer();

        FloatBuffer buffer = uploadBuffer;

        buffer.clear();

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

            /*
             * Min.w：
             * 保存该 Cuboid 所属的 Region Index。
             */
            buffer.put(
                    (float) CUBOID_REGION_INDICES[i]
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
         * 检查 Texture
         * ====================================================
         */
        if (!buffer.isDirect()) {
            throw new IllegalStateException(
                    "GhostIsolationGpuData upload buffer is not direct"
            );
        }

        int expectedFloats =
                TEXTURE_WIDTH
                        * MAX_CUBOIDS
                        * 4;

        if (buffer.capacity() < expectedFloats) {
            throw new IllegalStateException(
                    "GhostIsolationGpuData upload buffer capacity="
                            + buffer.capacity()
                            + ", expected="
                            + expectedFloats
            );
        }

        if (buffer.remaining() != expectedFloats) {
            throw new IllegalStateException(
                    "GhostIsolationGpuData upload buffer remaining="
                            + buffer.remaining()
                            + ", expected="
                            + expectedFloats
            );
        }

        /*
         * ====================================================
         * 上传 RGBA32F Texture
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
        dirty = false;
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
     * 确保 GPU 上传 Buffer 存在。
     */
    private static void ensureUploadBuffer() {

        if (uploadBuffer != null) {
            return;
        }

        uploadBuffer = org.lwjgl.system.MemoryUtil.memAllocFloat(
                TEXTURE_WIDTH
                        * MAX_CUBOIDS
                        * 4
        );

        QisPlan2.LOGGER.info(
                "[灵异隔绝 GPU] 创建上传 Buffer：{} floats",
                uploadBuffer.capacity()
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

            textureId = 0;
        }

        if (uploadBuffer != null) {

            org.lwjgl.system.MemoryUtil.memFree(
                    uploadBuffer
            );

            uploadBuffer = null;
        }

        cuboidCount = 0;

        dirty = true;

        clearCpuData();

        QisPlan2.LOGGER.info(
                "[灵异隔绝 GPU] 数据纹理已销毁"
        );
    }
}