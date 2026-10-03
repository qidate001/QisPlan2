package com.qidate.qisplan2.ghost.domain.client.renderer;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class GhostDomainRenderManager {

    private GhostDomainRenderManager() {
    }

    /**
     * 渲染当前所有需要显示的鬼域后处理效果。
     *
     * <p>鬼域后处理统一经过 Stencil Test，
     * 灵异隔绝空间内的像素不会受到鬼域效果影响。</p>
     *
     * <p>当前帧的渲染流程：</p>
     *
     * <ol>
     *     <li>收集当前需要渲染的鬼域后处理效果</li>
     *     <li>准备共享 Depth</li>
     *     <li>根据灵异隔绝空间生成 Stencil Mask</li>
     *     <li>渲染鬼域后处理效果</li>
     *     <li>恢复 Stencil 状态</li>
     * </ol>
     */
    public static void render() {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        /*
         * 主渲染目标必须启用 Stencil。
         *
         * GhostDomainStencil 会使用主渲染目标的
         * Stencil Buffer 标记灵异隔绝空间。
         */
        minecraft.getMainRenderTarget().enableStencil();

        /*
         * ========================================================
         * 1. 找出当前需要渲染的鬼域后处理效果
         * ========================================================
         */
        List<GhostDomainRenderEffect> effects =
                new ArrayList<>();

        for (GhostDomainRenderEffect effect :
                GhostDomainRenderEffectRegistry.getEffects()) {

            if (effect.shouldRender(minecraft)) {
                effects.add(effect);
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
         * ========================================================
         * 2. 准备共享 Depth
         * ========================================================
         *
         * 所有鬼域后处理效果共享这一份 Depth。
         *
         * Depth 用于：
         * - 重建当前屏幕像素对应的世界坐标
         * - 判断摄像机到可见表面的射线
         * - 计算灵异隔绝空间
         */
        GhostDomainRenderPipeline.prepareDepth();

        /*
         * ========================================================
         * 3. 根据灵异隔绝空间写入 Stencil
         * ========================================================
         *
         * GhostIsolationCuboid 会通过 GPU 计算当前像素
         * 是否处于灵异隔绝空间。
         *
         * Stencil 约定：
         *
         *     0 = 正常空间
         *     1 = 灵异隔绝空间
         *
         * 后续鬼域后处理使用：
         *
         *     Stencil == 0
         *
         * 因此隔绝空间内不会渲染鬼域效果。
         */
        GhostDomainStencil.writeIsolationStencil();

        /*
         * ========================================================
         * 3.5. 生成隔绝空间 Region 身份纹理
         * ========================================================
         *
         * Stencil 只回答：
         *
         *     “这个像素是不是处于隔绝空间？”
         *
         * Region Identity 则进一步回答：
         *
         *     “这个像素属于哪个隔绝空间？”
         *
         * 两者职责不同。
         */
        GhostDomainStencil.writeIsolationRegion();

        /*
         * ========================================================
         * 4. 渲染鬼域后处理效果
         * ========================================================
         *
         * 不同 GhostDomain Effect
         * 可以选择不同的空间裁剪方式。
         *
         * 默认 Effect：
         *
         *     使用 Stencil
         *     Stencil == 0 → 渲染
         *
         * 特殊 Effect：
         *
         *     不使用 Stencil
         *     自行通过 Region Identity
         *     判断当前像素所属空间。
         */
        for (GhostDomainRenderEffect effect :
                effects) {

            /*
             * ====================================================
             * 根据当前 Effect 决定是否使用 Stencil
             * ====================================================
             */
            if (effect.useIsolationStencil()) {

                /*
                 * 普通鬼域效果：
                 *
                 *     Stencil == 0
                 *         → 允许渲染
                 *
                 *     Stencil == 1
                 *         → 禁止渲染
                 *
                 * 正常情况下这里已经是正确状态，
                 * 这里显式恢复一次可以避免前一个
                 * Region Identity Effect 修改状态。
                 */
                GhostDomainStencil.enableForEffect();

            } else {

                /*
                 * Region Identity Effect：
                 *
                 * 当前 Effect 不使用传统 Stencil。
                 *
                 * 关闭 Stencil Test，
                 * 但保留 Stencil Buffer 中已经生成的
                 * 隔绝空间信息。
                 */
                GhostDomainStencil.disableForEffect();
            }

            /*
             * ====================================================
             * 执行当前鬼域 Effect
             * ====================================================
             */
            GhostDomainRenderPipeline.renderPrepared(
                    effect,
                    minecraft
            );
        }

        /*
         * ========================================================
         * 5. 恢复 Stencil 状态
         * ========================================================
         */
        GhostDomainStencil.disable();
    }
}