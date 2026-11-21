package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomain;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;

public interface GhostDomainRenderEffect {

    /**
     * 获取该后处理效果使用的 Shader。
     */
    ResourceLocation shaderId();

    /**
     * 判断该后处理效果当前是否需要渲染。
     */
    boolean shouldRender(
            Minecraft minecraft
    );

    /**
     * 获取当前 Effect 对应的所有 GhostDomain。
     *
     * <p>
     * 默认按照 Shader ID / Domain Type 匹配。
     * </p>
     *
     * <p>
     * 一个 Effect 可以同时对应多个同类型 GhostDomain。
     * </p>
     */
    default Collection<ClientGhostDomain> getDomains(
            Minecraft minecraft
    ) {

        if (minecraft.level == null) {
            return Collections.emptyList();
        }

        ResourceLocation domainType =
                shaderId();

        ResourceLocation currentDimension =
                minecraft.level
                        .dimension()
                        .location();

        return ClientGhostDomainManager.getDomains()
                .stream()
                .filter(domain ->
                        domain.getDimension()
                                .equals(currentDimension)
                )
                .filter(domain ->
                        domainType.equals(
                                domain.getDomainType()
                        )
                )
                .toList();
    }

    /**
     * 绑定当前效果专属的 Shader 采样纹理。
     *
     * 公共颜色和深度输入已由管线准备；本方法随后执行，早于 setupUniforms 和绘制。
     * 效果负责使 sampler 名称与自己的 Shader 声明一致，不通过其他 uniform 推测纹理需求。
     * 每个 Domain 绘制前重新取得并绑定纹理，避免窗口变化后沿用失效的纹理 ID。
     * 不需要额外纹理的效果直接使用默认空实现。
     *
     * @param shader 当前效果的 Shader
     * @param minecraft 当前 Minecraft 实例
     * @param domain 当前正在渲染的 GhostDomain
     */
    default void setupSamplers(
            ShaderInstance shader,
            Minecraft minecraft,
            ClientGhostDomain domain
    ) {
    }

    /**
     * 设置指定 GhostDomain 对应的 Shader Uniform。
     *
     * @param shader 当前 Shader
     * @param minecraft 当前 Minecraft 实例
     * @param domain 当前正在渲染的 GhostDomain
     */
    void setupUniforms(
            ShaderInstance shader,
            Minecraft minecraft,
            ClientGhostDomain domain
    );

    /**
     * 是否使用灵异隔绝 Stencil。
     */
    default boolean useIsolationStencil() {
        return true;
    }
}