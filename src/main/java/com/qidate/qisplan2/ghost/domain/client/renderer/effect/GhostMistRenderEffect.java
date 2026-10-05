package com.qidate.qisplan2.ghost.domain.client.renderer.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomain;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainMatrices;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainRenderEffect;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainRenderEffectRegistry;
import com.qidate.qisplan2.ghost.domain.type.mist.GhostMistDomainController;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * ========================================================
 * 鬼雾鬼域渲染效果
 * ========================================================
 *
 * 鬼雾使用球形鬼域作为雾气范围。
 *
 * 主要视觉效果：
 *
 * 1. 球形鬼域内部产生雾气
 * 2. 根据视线穿过鬼域的长度计算雾浓度
 * 3. 使用程序噪声制造不均匀的雾气
 * 4. 使用时间参数让雾气产生缓慢流动
 * 5. 使用 MainDepthSampler 让场景物体正确遮挡雾
 *
 * 空间隔离：
 *
 * 使用 GhostDomain Source Region Identity，
 * 与 GhostEye 保持一致。
 */
public final class GhostMistRenderEffect
        implements GhostDomainRenderEffect {

    private static final GhostMistRenderEffect INSTANCE =
            new GhostMistRenderEffect();

    private GhostMistRenderEffect() {
    }

    /**
     * ========================================================
     * 注册
     * ========================================================
     */
    public static void register() {

        GhostDomainRenderEffectRegistry.register(
                INSTANCE
        );
    }

    public static GhostMistRenderEffect getInstance() {
        return INSTANCE;
    }

    /**
     * ========================================================
     * Shader ID
     * ========================================================
     */
    @Override
    public ResourceLocation shaderId() {

        return GhostMistDomainController.DOMAIN_TYPE;
    }

    /**
     * ========================================================
     * 是否渲染
     * ========================================================
     */
    @Override
    public boolean shouldRender(
            Minecraft minecraft
    ) {

        if (minecraft.level == null) {
            return false;
        }

        return getCurrentGhostMistDomain(
                minecraft
        ) != null;
    }

    /**
     * ========================================================
     * Isolation Stencil
     * ========================================================
     *
     * 鬼雾和鬼眼一样，
     * 使用 Region Identity 判断空间归属。
     */
    @Override
    public boolean useIsolationStencil() {
        return false;
    }

    /**
     * ========================================================
     * Shader Uniform
     * ========================================================
     */
    @Override
    public void setupUniforms(
            ShaderInstance shader,
            Minecraft minecraft
    ) {

        ClientGhostDomain ghostMistDomain =
                getCurrentGhostMistDomain(
                        minecraft
                );

        if (ghostMistDomain == null) {
            return;
        }

        /*
         * ========================================================
         * Projection / ModelView
         * ========================================================
         */

        shader.getUniform(
                "GhostMistProjMat"
        ).set(
                RenderSystem.getProjectionMatrix()
        );

        shader.getUniform(
                "GhostMistModelViewMat"
        ).set(
                GhostDomainMatrices.getModelViewMatrix()
        );

        /*
         * ========================================================
         * 摄像机位置
         * ========================================================
         */

        Camera camera =
                minecraft.gameRenderer
                        .getMainCamera();

        shader.getUniform(
                "GhostMistCameraPos"
        ).set(
                (float) camera.getPosition().x,
                (float) camera.getPosition().y,
                (float) camera.getPosition().z
        );

        /*
         * ========================================================
         * 鬼域中心
         * ========================================================
         */

        shader.getUniform(
                "GhostMistDomainCenter"
        ).set(
                (float) ghostMistDomain.getX(),
                (float) ghostMistDomain.getY(),
                (float) ghostMistDomain.getZ()
        );

        /*
         * ========================================================
         * 鬼域半径
         * ========================================================
         */

        shader.getUniform(
                "GhostMistDomainRadius"
        ).set(
                (float) ghostMistDomain.getRenderRadius()
        );

        /*
         * ========================================================
         * 鬼域启用状态
         * ========================================================
         */

        shader.getUniform(
                "GhostMistDomainActive"
        ).set(1.0F);

        /*
         * ========================================================
         * 鬼域强度（浓度）
         * ========================================================
         */

        shader.getUniform(
                "GhostMistDomainStrength"
        ).set(
                (float) ghostMistDomain.getStrength()
        );

        /*
         * ========================================================
         * 时间
         * ========================================================
         *
         * 用于 Shader 中的雾气缓慢流动。
         */
        float time =
                (minecraft.level.getGameTime() % 24000L)
                        / 20.0F;

        shader.getUniform(
                "GhostMistTime"
        ).set(time);

        /*
         * ========================================================
         * Source Region Identity
         * ========================================================
         *
         * 0：
         *     开放空间
         *
         * 1：
         *     GPU Region Index 0
         *
         * 2：
         *     GPU Region Index 1
         *
         * ...
         */

        UUID sourceRegionUUID =
                ghostMistDomain.getSourceRegionUUID();

        if (sourceRegionUUID == null) {

            shader.getUniform(
                    "GhostMistSourceRegion"
            ).set(
                    0.0F
            );

        } else {

            int regionIndex =
                    ClientGhostIsolationManager.getRegionIndex(
                            sourceRegionUUID
                    );

            float encodedSourceRegion =
                    (float) (regionIndex + 1)
                            / 256.0F;

            shader.getUniform(
                    "GhostMistSourceRegion"
            ).set(
                    encodedSourceRegion
            );
        }
    }

    /**
     * ========================================================
     * 获取当前鬼雾鬼域
     * ========================================================
     */
    private ClientGhostDomain getCurrentGhostMistDomain(
            Minecraft minecraft
    ) {

        if (minecraft.level == null) {
            return null;
        }

        return ClientGhostDomainManager.getFirstDomainByType(
                GhostMistDomainController.DOMAIN_TYPE
        );
    }
}