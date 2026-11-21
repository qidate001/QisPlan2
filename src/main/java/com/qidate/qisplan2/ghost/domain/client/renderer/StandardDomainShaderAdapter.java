package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainShaderEffect;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import java.io.IOException;
import java.util.Set;

/** 标准后处理实现。只有选择 GhostDomainShaderEffect 的模块才使用前缀参数约定。 */
public final class StandardDomainShaderAdapter implements GhostDomainClientEffect<GhostDomainShaderEffect> {
    /** 将鬼域类型、着色器资源和参数前缀分开绑定，允许多个模块复用同一资源。 */
    @Override public void install(ResourceLocation moduleId,GhostDomainShaderEffect definition) {
        GhostDomainRenderEffectRegistry.register(new ModuleGhostDomainRenderEffect(moduleId,definition.shaderId(),definition.uniformPrefix()));
    }
    /** 注册或重载资源；相同着色器 ID 只加载一次。 */
    @Override public void registerShaders(RegisterShadersEvent event,GhostDomainShaderEffect definition,Set<ResourceLocation> loaded) throws IOException {
        if(!loaded.add(definition.shaderId())) return;
        event.registerShader(new ShaderInstance(event.getResourceProvider(),definition.shaderId(),DefaultVertexFormat.POSITION),
                shader -> GhostDomainShaderRegistry.register(definition.shaderId(),shader));
    }
}
