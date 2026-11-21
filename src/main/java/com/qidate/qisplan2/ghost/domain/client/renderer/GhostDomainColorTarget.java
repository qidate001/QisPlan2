package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * 鬼域每一遍后处理的只读颜色快照。
 * 深度整帧共用，但颜色必须在每个 Domain 绘制前刷新，包含之前已完成的叠加效果。
 * 采样本副本、写入主目标，避免同一颜色附件同时被读写造成反馈闪烁。
 */
public final class GhostDomainColorTarget {
    private static TextureTarget target;

    /** 工具类不创建实例。 */
    private GhostDomainColorTarget() {}

    /**
     * 复制主目标的完整颜色，返回与主目标附件不同的采样纹理。
     * 只复制颜色，不覆盖主目标的深度、Stencil 或独立 Region Identity 纹理。
     * 创建缓冲和 blit 都会改变 FBO，因此无论正常完成还是抛出异常都恢复主目标及 viewport。
     */
    public static int capture(RenderTarget mainTarget) {
        RenderSystem.assertOnRenderThreadOrInit();
        try {
            if(target==null || target.width!=mainTarget.width || target.height!=mainTarget.height) {
                if(target!=null) target.destroyBuffers();
                target=new TextureTarget(mainTarget.width,mainTarget.height,false,Minecraft.ON_OSX);
            }

            // Scissor 会裁剪 framebuffer blit。局部关闭后原样恢复，不能遗留未更新的角落。
            // 此区间只调用原生 GL；结束时恢复原状态，不改变 RenderSystem 的状态缓存。
            boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            try {
                if(scissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,mainTarget.frameBufferId);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,target.frameBufferId);
                GlStateManager._glBlitFrameBuffer(0,0,mainTarget.width,mainTarget.height,
                        0,0,target.width,target.height,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
            } finally {
                if(scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }
            return target.getColorTextureId();
        } finally {
            mainTarget.bindWrite(true);
        }
    }

    /** 退出世界时释放显存；重新进入会按当前主目标尺寸懒创建。 */
    public static void destroy() {
        RenderSystem.assertOnRenderThreadOrInit();
        if(target==null) return;
        try {
            target.destroyBuffers();
            target=null;
        } finally {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }
}
