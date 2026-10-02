package com.qidate.qisplan2.ghost.isolation.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

/**
 * 灵异隔绝 Mask 使用的 RenderTarget。
 *
 * <p>
 * 该 RenderTarget 保存当前帧的灵异隔绝空间 Mask。
 * </p>
 *
 * <p>
 * Mask 使用红色通道表示隔绝状态：
 * </p>
 *
 * <ul>
 *     <li>0.0：当前位置不属于灵异隔绝空间</li>
 *     <li>1.0：当前位置属于灵异隔绝空间</li>
 * </ul>
 *
 * <p>
 * 后续 GhostEye、鬼雨等鬼域后处理效果，
 * 都可以读取这份共享 Mask。
 * </p>
 */
public final class GhostIsolationMaskTarget {

    private static TextureTarget TARGET;

    private GhostIsolationMaskTarget() {
    }

    /**
     * 获取当前窗口尺寸对应的灵异隔绝 Mask。
     *
     * <p>
     * 如果窗口尺寸发生变化，
     * 则重新创建 RenderTarget。
     * </p>
     *
     * @return 当前灵异隔绝 Mask RenderTarget
     */
    public static TextureTarget get() {

        Minecraft minecraft =
                Minecraft.getInstance();

        int width =
                minecraft.getMainRenderTarget().width;

        int height =
                minecraft.getMainRenderTarget().height;

        /*
         * 窗口尺寸发生变化时，
         * 原来的 Mask 已经不能继续使用。
         */
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
     * 销毁灵异隔绝 Mask RenderTarget。
     *
     * <p>
     * 客户端退出世界或释放渲染资源时调用。
     * </p>
     */
    public static void destroy() {

        if (TARGET != null) {

            TARGET.destroyBuffers();
            TARGET = null;
        }
    }
}