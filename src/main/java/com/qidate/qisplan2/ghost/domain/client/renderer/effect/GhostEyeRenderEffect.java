package com.qidate.qisplan2.ghost.domain.client.renderer.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qidate.qisplan2.ghost.isolation.client.renderer.GhostIsolationMaskTarget;
import com.qidate.qisplan2.ghost.possession.ability.ghosteye.GhostEyeAbility;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomain;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainMatrices;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainRenderEffect;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainRenderEffectRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

public final class GhostEyeRenderEffect
        implements GhostDomainRenderEffect {

    private static final GhostEyeRenderEffect INSTANCE =
            new GhostEyeRenderEffect();

    private GhostEyeRenderEffect() {
    }

    public static void register() {

        GhostDomainRenderEffectRegistry.register(
                INSTANCE
        );
    }

    public static GhostEyeRenderEffect getInstance() {
        return INSTANCE;
    }

    @Override
    public ResourceLocation shaderId() {
        return GhostEyeAbility.ID;
    }

    @Override
    public boolean shouldRender(
            Minecraft minecraft
    ) {

        if (minecraft.level == null) {
            return false;
        }

        return getCurrentGhostEyeDomain(
                minecraft
        ) != null;
    }

//    @Override
//    public void setupUniforms(
//            ShaderInstance shader,
//            Minecraft minecraft
//    ) {
//
//        ClientGhostDomain ghostEyeDomain =
//                getCurrentGhostEyeDomain(
//                        minecraft
//                );
//
//        if (ghostEyeDomain == null) {
//            return;
//        }
//
//        /*
//         * ========================================================
//         * 灵异隔绝 Mask
//         * ========================================================
//         *
//         * IsolationMaskSampler 是 sampler2D，
//         * 不能通过 getUniform().set() 设置。
//         *
//         * 这里直接把灵异隔绝 Mask RenderTarget
//         * 的颜色纹理绑定给 GhostEye Shader。
//         */
//        shader.setSampler(
//                "IsolationMaskSampler",
//                GhostIsolationMaskTarget.get().getColorTextureId()
//        );
//
//        /*
//         * ========================================================
//         * Projection / ModelView
//         * ========================================================
//         */
//
//        shader.getUniform(
//                "GhostEyeProjMat"
//        ).set(
//                RenderSystem.getProjectionMatrix()
//        );
//
//        shader.getUniform(
//                "GhostEyeModelViewMat"
//        ).set(
//                GhostDomainMatrices.getModelViewMatrix()
//        );
//
//        /*
//         * ========================================================
//         * 摄像机位置
//         * ========================================================
//         */
//
//        Camera camera =
//                minecraft.gameRenderer
//                        .getMainCamera();
//
//        shader.getUniform(
//                "GhostEyeCameraPos"
//        ).set(
//                (float) camera.getPosition().x,
//                (float) camera.getPosition().y,
//                (float) camera.getPosition().z
//        );
//
//        /*
//         * ========================================================
//         * 鬼域中心
//         * ========================================================
//         */
//
//        shader.getUniform(
//                "GhostEyeDomainCenter"
//        ).set(
//                (float) ghostEyeDomain.getX(),
//                (float) ghostEyeDomain.getY(),
//                (float) ghostEyeDomain.getZ()
//        );
//
//        /*
//         * ========================================================
//         * 鬼域半径
//         * ========================================================
//         */
//
//        shader.getUniform(
//                "GhostEyeDomainRadius"
//        ).set(
//                (float) ghostEyeDomain.getRenderRadius()
//        );
//
//        /*
//         * ========================================================
//         * 鬼域启用状态
//         * ========================================================
//         */
//
//        shader.getUniform(
//                "GhostEyeDomainActive"
//        ).set(1.0F);
//
//        /*
//         * ========================================================
//         * 鬼域层级
//         * ========================================================
//         */
//
//        shader.getUniform(
//                "GhostEyeDomainLayer"
//        ).set(
//                (float) ghostEyeDomain.getLayer()
//        );
//    }

    @Override
    public void setupUniforms(
            ShaderInstance shader,
            Minecraft minecraft
    ) {

        ClientGhostDomain ghostEyeDomain =
                getCurrentGhostEyeDomain(
                        minecraft
                );

        if (ghostEyeDomain == null) {
            return;
        }

        /*
         * ========================================================
         * 灵异隔绝 Mask
         * ========================================================
         *
         * 当前正在调试 Mask。
         *
         * ghost_eye.fsh 此时只输出 IsolationMaskSampler，
         * 因此原本的 GhostEye Uniform 已经不会被 Shader 保留。
         *
         * 这里暂时只绑定 Mask 纹理。
         */
        shader.setSampler(
                "IsolationMaskSampler",
                GhostIsolationMaskTarget.get().getColorTextureId()
        );
    }

    private ClientGhostDomain getCurrentGhostEyeDomain(
            Minecraft minecraft
    ) {

        if (minecraft.level == null) {
            return null;
        }

        return ClientGhostDomainManager.getFirstDomainByType(
                GhostEyeAbility.ID
        );
    }
}