package com.qidate.qisplan2.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

public final class GhostEyeDepthTarget {

    private static TextureTarget TARGET;

    private GhostEyeDepthTarget() {
    }

    /**
     * 鬼眼专用深度缓冲。
     *
     * <p>这里保存的是 MainRenderTarget 的深度副本。</p>
     *
     * <p>不能直接采样 MainRenderTarget 的 Depth Attachment，
     * 否则会触发 Framebuffer Feedback，
     * 导致鬼域撕裂、小窗口斜边闪烁等问题。</p>
     */
    public static TextureTarget get() {

        Minecraft mc = Minecraft.getInstance();

        int width = mc.getMainRenderTarget().width;
        int height = mc.getMainRenderTarget().height;

        if (TARGET == null
                || TARGET.width != width
                || TARGET.height != height) {

            if (TARGET != null) {
                TARGET.destroyBuffers();
            }

            TARGET = new TextureTarget(
                    width,
                    height,
                    true,   // 有深度
                    Minecraft.ON_OSX
            );
        }

        return TARGET;
    }

    public static void destroy() {

        if (TARGET != null) {

            TARGET.destroyBuffers();
            TARGET = null;
        }
    }
}