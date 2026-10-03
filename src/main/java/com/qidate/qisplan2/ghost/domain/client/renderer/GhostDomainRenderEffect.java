package com.qidate.qisplan2.ghost.domain.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

public interface GhostDomainRenderEffect {

    /**
     * 当前渲染效果使用的 Shader ID。
     */
    ResourceLocation shaderId();

    /**
     * 判断当前是否应该渲染这个效果。
     */
    boolean shouldRender(
            Minecraft minecraft
    );

    /**
     * 设置该渲染效果专属的 Shader Uniform。
     */
    void setupUniforms(
            ShaderInstance shader,
            Minecraft minecraft
    );

    /**
     * 判断当前渲染效果是否使用
     * 灵异隔绝空间 Stencil Mask。
     *
     * <p>
     * 默认情况下，鬼域后处理使用传统的
     * Stencil 空间裁剪规则：
     *
     * <pre>
     * Stencil = 0
     *     → 普通空间
     *     → 允许鬼域效果
     *
     * Stencil = 1
     *     → 灵异隔绝空间
     *     → 不允许鬼域效果
     * </pre>
     *
     * <p>
     * 对于已经能够通过
     * {@code GhostDomainRegionIdentity}
     * 自行判断空间身份的特殊效果，
     * 可以返回 {@code false}。
     *
     * <p>
     * 例如 GhostEye：
     *
     * <pre>
     * PixelRegion == SourceRegion
     *     → 允许渲染
     *
     * PixelRegion != SourceRegion
     *     → 保持原画面
     * </pre>
     *
     * @return 是否使用传统 Stencil Mask
     */
    default boolean useIsolationStencil() {
        return true;
    }
}