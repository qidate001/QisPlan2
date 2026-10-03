package com.qidate.qisplan2.ghost.isolation.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

/**
 * 灵异隔绝空间 Region 身份纹理。
 *
 * <p>
 * 该 Target 用于保存屏幕上每个像素所属的隔绝空间身份。
 *
 * <p>
 * 纹理语义：
 *
 * <ul>
 *     <li>0：普通空间</li>
 *     <li>非 0：对应的 Region 身份</li>
 * </ul>
 *
 * <p>
 * 当前阶段仅负责创建和维护 Render Target。
 * 实际 Region 身份写入将在后续 Pass 中完成。
 */
public final class GhostIsolationRegionTarget {

    private static TextureTarget TARGET;

    private GhostIsolationRegionTarget() {
    }

    /**
     * 获取当前尺寸的 Region Identity Target。
     *
     * <p>
     * 如果窗口尺寸发生变化，则重新创建。
     *
     * @return Region Identity Target
     */
    public static TextureTarget get() {

        Minecraft minecraft =
                Minecraft.getInstance();

        int width =
                minecraft.getMainRenderTarget().width;

        int height =
                minecraft.getMainRenderTarget().height;

        /*
         * 第一次创建，
         * 或窗口尺寸发生变化时重新创建。
         */
        if (TARGET == null
                || TARGET.width != width
                || TARGET.height != height) {

            if (TARGET != null) {
                TARGET.destroyBuffers();
            }

            TARGET =
                    new TextureTarget(
                            width,
                            height,
                            false,
                            Minecraft.ON_OSX
                    );
        }

        return TARGET;
    }

    /**
     * 销毁 Region Identity Target。
     */
    public static void destroy() {

        if (TARGET != null) {

            TARGET.destroyBuffers();

            TARGET = null;
        }
    }
}