package com.qidate.qisplan2.ghost.isolation.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

/**
 * 灵异隔绝 Mask 的独立 GPU RenderTarget。
 *
 * 颜色纹理：
 *
 *     白色 = 灵异隔绝
 *     黑色 = 非灵异隔绝
 *
 * 该 Target 不直接显示到主屏幕，
 * 而是作为其他鬼域 Shader 的输入纹理。
 */
public final class GhostIsolationMaskTarget {

    private static TextureTarget TARGET;

    private GhostIsolationMaskTarget() {
    }

    /**
     * 获取当前尺寸对应的 Mask Target。
     *
     * 如果窗口尺寸发生变化，
     * 自动重新创建。
     */
    public static TextureTarget get() {

        Minecraft minecraft = Minecraft.getInstance();

        int width =
                minecraft.getMainRenderTarget().width;

        int height =
                minecraft.getMainRenderTarget().height;

        if (TARGET == null
                || TARGET.width != width
                || TARGET.height != height) {

            if (TARGET != null) {
                TARGET.destroyBuffers();
            }

            TARGET = new TextureTarget(
                    width,
                    height,
                    false,
                    Minecraft.ON_OSX
            );
        }

        return TARGET;
    }

    /**
     * 获取 Mask 颜色纹理 ID。
     *
     * GhostEye / GhostRain Shader
     * 将通过这个 Texture 读取 Mask。
     */
    public static int getTextureId() {

        return get().getColorTextureId();
    }

    /**
     * 销毁 GPU Target。
     */
    public static void destroy() {

        if (TARGET != null) {

            TARGET.destroyBuffers();

            TARGET = null;
        }
    }
}