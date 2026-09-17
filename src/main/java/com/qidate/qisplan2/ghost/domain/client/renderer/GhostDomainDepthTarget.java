package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

/**
 * 鬼域后处理共用的深度缓冲。
 *
 * <p>
 * 这里保存的是 MainRenderTarget 当前帧的深度副本。
 * 所有需要读取世界深度的鬼域后处理都应该使用这里的深度纹理，
 * 而不是直接采样 MainRenderTarget 的 Depth Attachment。
 * </p>
 *
 * <p>
 * 不能在向 MainRenderTarget 写入颜色的同时，
 * 直接采样它自己的 Depth Attachment。
 * </p>
 *
 * <p>
 * 这种 Framebuffer Feedback 会导致：
 * <ul>
 *     <li>小窗口下出现撕裂</li>
 *     <li>特定视角出现斜向撕裂</li>
 *     <li>飞行时撕裂加重</li>
 *     <li>深度区域出现不稳定闪烁</li>
 * </ul>
 * </p>
 *
 * <p>
 * 因此每帧后处理开始前，
 * 会先使用 {@link TextureTarget#copyDepthFrom} 将主渲染目标的深度
 * 复制到这里，再由各个鬼域 Shader 采样这个独立的深度纹理。
 * </p>
 */
public final class GhostDomainDepthTarget {

    private static TextureTarget TARGET;

    private GhostDomainDepthTarget() {
    }

    /**
     * 获取当前窗口尺寸对应的鬼域深度缓冲。
     *
     * <p>
     * 如果窗口尺寸发生变化，则重新创建 RenderTarget。
     * </p>
     */
    public static TextureTarget get() {

        Minecraft minecraft =
                Minecraft.getInstance();

        int width =
                minecraft.getMainRenderTarget().width;

        int height =
                minecraft.getMainRenderTarget().height;

        /*
         * 窗口大小发生变化时，
         * 原来的深度缓冲已经不能继续使用。
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
                    true,
                    Minecraft.ON_OSX
            );
        }

        return TARGET;
    }

    /**
     * 销毁鬼域深度缓冲。
     *
     * <p>
     * 客户端退出世界、关闭资源或需要释放渲染资源时调用。
     * </p>
     */
    public static void destroy() {

        if (TARGET != null) {

            TARGET.destroyBuffers();
            TARGET = null;
        }
    }
}