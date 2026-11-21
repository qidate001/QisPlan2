package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.qidate.qisplan2.ghost.module.domain.GhostDomainEffect;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import java.io.IOException;
import java.util.Set;

/** 声明到客户端实现的适配契约；实现可安装后处理、粒子、声音或自己的渲染事件。 */
@FunctionalInterface
public interface GhostDomainClientEffect<E extends GhostDomainEffect> {
    /** 为声明此效果的模块安装表现；moduleId 是鬼域类型，不必等于着色器资源 ID。 */
    void install(ResourceLocation moduleId, E definition);

    /** 资源重载时加载本效果的着色器。loaded 跨适配器共享，避免重复加载同一资源。 */
    default void registerShaders(RegisterShadersEvent event, E definition, Set<ResourceLocation> loaded) throws IOException {}
}
