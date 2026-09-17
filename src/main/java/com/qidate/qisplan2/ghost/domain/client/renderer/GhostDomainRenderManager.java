package com.qidate.qisplan2.ghost.domain.client.renderer;

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
         * 当前没有任何鬼域后处理需要渲染。
         */
        if (effects.isEmpty()) {
            return;
        }

        /*
         * ========================================================
         * 准备当前帧深度
         * ========================================================
         *
         * 所有鬼域后处理共享这一份深度副本。
         *
         * 无论这一帧有多少个 Effect，
         * 深度只复制一次。
         */
        GhostDomainRenderPipeline.prepareDepth();

        /*
         * ========================================================
         * 依次执行所有鬼域后处理
         * ========================================================
         */

        for (GhostDomainRenderEffect effect :
                effects) {

            GhostDomainRenderPipeline.renderPrepared(
                    effect,
                    minecraft
            );
        }
    }
}