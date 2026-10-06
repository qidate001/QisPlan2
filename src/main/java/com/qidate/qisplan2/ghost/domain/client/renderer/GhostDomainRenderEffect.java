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