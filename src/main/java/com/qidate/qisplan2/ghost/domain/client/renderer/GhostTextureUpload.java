package com.qidate.qisplan2.ghost.domain.client.renderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

/** 鬼域与隔绝空间共用的浮点数据纹理上传器；调用方负责绑定目标纹理。 */
public final class GhostTextureUpload {
    private GhostTextureUpload() {}

    /**
     * 上传紧密排列的 RGBA 浮点数据，并完整恢复调用前的像素解包状态。
     * 原版图集上传可能遗留行跨度和跳过像素；直接继承这些状态会让驱动越界读取
     * 本地缓冲区，造成无法由 Java 捕获的原生崩溃。CPU 地址也不能与 PBO 绑定混用。
     * @param replaceStorage true 时分配纹理存储，false 时更新已有的同尺寸存储
     */
    public static void rgba32f(int width, int height, FloatBuffer data, boolean replaceStorage) {
        if (width <= 0 || height <= 0 || !data.isDirect()
                || data.remaining() != Math.multiplyExact(Math.multiplyExact(width, height), 4)) {
            throw new IllegalArgumentException("RGBA32F requires tightly packed direct pixel data");
        }
        int[] names = {GL11.GL_UNPACK_ALIGNMENT, GL11.GL_UNPACK_ROW_LENGTH,
                GL11.GL_UNPACK_SKIP_ROWS, GL11.GL_UNPACK_SKIP_PIXELS, GL11.GL_UNPACK_SWAP_BYTES};
        int[] previous = new int[names.length];
        for (int i = 0; i < names.length; i++) previous[i] = GL11.glGetInteger(names[i]);
        int buffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            for (int i = 0; i < names.length; i++) GL11.glPixelStorei(names[i], i == 0 ? 4 : 0);
            if (replaceStorage) {
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F,
                        width, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, data);
            } else {
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                        width, height, GL11.GL_RGBA, GL11.GL_FLOAT, data);
            }
        } finally {
            for (int i = 0; i < names.length; i++) GL11.glPixelStorei(names[i], previous[i]);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, buffer);
        }
    }
}
