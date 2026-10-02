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

        minecraft.getMainRenderTarget().enableStencil();

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
         * 没有任何鬼域后处理效果时，
         * 当前帧不需要执行后处理。
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
         * GhostIsolationMask
         * GhostEye
         * GhostRain
         *
         * 都使用这一份独立 Depth。
         */
        GhostDomainRenderPipeline.prepareDepth();

        GhostDomainStencil.writeTestMask();

        /*
         * ====================================================
         * 3. 正常鬼域后处理
         * ====================================================
         *
         * 此时：
         *
         * GhostIsolationMaskTarget
         *
         * 已经准备完成。
         *
         * 后续鬼域效果可以直接读取
         * IsolationMaskSampler。
         */
        for (GhostDomainRenderEffect effect :
                effects) {

            GhostDomainRenderPipeline.renderPrepared(
                    effect,
                    minecraft
            );
        }



        GhostDomainStencil.disable();

        /*
         * ====================================================
         * 2. 生成灵异隔绝 Mask
         * ====================================================
         *
         * Mask 必须在所有鬼域后处理之前生成。
         *
         * GhostEye / GhostRain 等后续效果
         * 会读取这张 Mask，
         * 从而判断某个像素是否处于
         * 灵异隔绝空间。
         */
//        GhostIsolationMaskRenderer.render();
    }
}