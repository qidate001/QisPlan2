package com.qidate.qisplan2.ghost.domain.client.renderer;

public final class GhostDomainRenderManager {

    private GhostDomainRenderManager() {
    }

    /**
     * 渲染当前所有需要显示的鬼域后处理效果。
     */
    public static void render() {

        for (GhostDomainRenderEffect effect :
                GhostDomainRenderEffectRegistry.getEffects()) {

            GhostDomainRenderPipeline.render(
                    effect
            );
        }
    }
}