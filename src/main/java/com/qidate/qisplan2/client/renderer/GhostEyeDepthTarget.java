package com.qidate.qisplan2.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

public final class GhostEyeDepthTarget {

    private static TextureTarget TARGET;

    private GhostEyeDepthTarget() {
    }

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