package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.qidate.qisplan2.ghost.isolation.client.renderer.GhostIsolationMaskRenderer;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class GhostDomainRenderManager {

    private GhostDomainRenderManager() {
    }

    /**
     * 渲染当前所有需要显示的鬼域后处理效果。
     */
    public static void render() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        /*
         * ========================================================
         * 找出当前需要渲染的效果
         * ========================================================
         */

        List<GhostDomainRenderEffect> effects =
                new ArrayList<>();

        for (GhostDomainRenderEffect effect :
                GhostDomainRenderEffectRegistry.getEffects()) {

            if (effect.shouldRender(
                    minecraft
            )) {
                effects.add(
                        effect
                );
            }
        }

        /*
         * 没有任何后处理效果时，
         * 当前帧不需要准备 Depth。
         */
        if (effects.isEmpty()) {
            return;
        }

        /*
         * ====================================================
         * 1. 准备共享 Depth
         * ====================================================
         *
         * 所有后处理效果，包括：
         *
         * GhostEye
         * GhostRain
         * GhostIsolationMask
         *
         * 都使用这一份独立 Depth。
         */
        GhostDomainRenderPipeline.prepareDepth();

        /*
         * ====================================================
         * 3. 正常鬼域后处理
         * ====================================================
         */
        for (GhostDomainRenderEffect effect :
                effects) {

            GhostDomainRenderPipeline.renderPrepared(
                    effect,
                    minecraft
            );
        }

        /*
         * ====================================================
         * 2. 生成灵异隔绝 Mask
         * ====================================================
         *
         * 当前阶段是 Debug 模式，
         * Mask 会直接输出到屏幕。
         */
        GhostIsolationMaskRenderer.render();
    }
}