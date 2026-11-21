package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomain;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationManager;
import com.qidate.qisplan2.ghost.isolation.client.GhostIsolationRegionTarget;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

/** 模块鬼域的标准渲染适配器。管线只认识效果接口，不认识任何具体厉鬼。 */
public record ModuleGhostDomainRenderEffect(ResourceLocation shaderId, String prefix) implements GhostDomainRenderEffect {
    /** 从已注册模块派生效果，不再为每只厉鬼手工注册。 */
    public static void registerModules() {
        for(var module:GhostModuleRegistry.getAll()) module.domainDefinition().ifPresent(d -> {
            if(!d.visualPrefix().isEmpty()) GhostDomainRenderEffectRegistry.register(new ModuleGhostDomainRenderEffect(module.getId(), d.visualPrefix()));
        });
    }

    /** 只在本模块的鬼域位于当前维度时绘制。 */
    @Override
    public boolean shouldRender(Minecraft minecraft) { return !getDomains(minecraft).isEmpty(); }

    /** 标准模块着色器使用 Region Identity 而非隔绝模板。 */
    @Override
    public boolean useIsolationStencil() { return false; }

    /** sampler 名称来自模块声明，不靠其他 uniform 猜测需要哪张纹理。 */
    @Override
    public void setupSamplers(ShaderInstance shader, Minecraft minecraft, ClientGhostDomain domain) {
        shader.setSampler(prefix + "RegionIdentity", GhostIsolationRegionTarget.get().getColorTextureId());
    }

    /** 标准着色器参数按声明前缀写入，未声明的可选输入不会被强制使用。 */
    @Override
    public void setupUniforms(ShaderInstance shader, Minecraft minecraft, ClientGhostDomain domain) {
        if (domain == null || minecraft.level == null) return;

        var projection = shader.getUniform(prefix + "ProjMat");
        if (projection != null)projection.set(RenderSystem.getProjectionMatrix());

        var view = shader.getUniform(prefix + "ModelViewMat");
        if (view != null)view.set(GhostDomainMatrices.getModelViewMatrix());

        var pos = minecraft.gameRenderer.getMainCamera().getPosition();
        var camera = shader.getUniform(prefix + "CameraPos");
        if (camera != null)camera.set((float)pos.x, (float)pos.y, (float)pos.z);

        var center = shader.getUniform(prefix + "DomainCenter");
        if (center != null)center.set((float)domain.getX(), (float)domain.getY(), (float)domain.getZ());

        scalar(shader,"DomainRadius", (float)domain.getRenderRadius());
        scalar(shader,"DomainActive", 1);
        scalar(shader,"DomainLayer", domain.getLayer());
        scalar(shader,"DomainStrength", (float)domain.getStrength());
        scalar(shader,"Time", (minecraft.level.getGameTime() % 24000) / 20F);

        var region = domain.getSourceRegionUUID();
        scalar(shader,"SourceRegion", region == null
                ? 0 : (ClientGhostIsolationManager.getRegionIndex(region) + 1) / 256F);
    }

    /** 写入可选标量，保留不同模块着色器自己的参数集合。 */
    private void scalar(ShaderInstance shader, String name, float value) {
        var uniform = shader.getUniform(prefix+name);
        if (uniform != null) uniform.set(value);
    }
}
