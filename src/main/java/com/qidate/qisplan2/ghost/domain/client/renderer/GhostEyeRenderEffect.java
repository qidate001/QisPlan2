package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qidate.qisplan2.ghost.ability.ghosteye.GhostEyeAbility;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomain;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
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
         * Projection / ModelView
         * ========================================================
         */

        shader.getUniform(
                "GhostEyeProjMat"
        ).set(
                RenderSystem.getProjectionMatrix()
        );

        shader.getUniform(
                "GhostEyeModelViewMat"
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
                "GhostEyeCameraPos"
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
                "GhostEyeDomainCenter"
        ).set(
                (float) ghostEyeDomain.getX(),
                (float) ghostEyeDomain.getY(),
                (float) ghostEyeDomain.getZ()
        );

        /*
         * ========================================================
         * 鬼域半径
         * ========================================================
         */

        shader.getUniform(
                "GhostEyeDomainRadius"
        ).set(
                (float) ghostEyeDomain.getRadius()
        );

        /*
         * ========================================================
         * 鬼域启用状态
         * ========================================================
         */

        shader.getUniform(
                "GhostEyeDomainActive"
        ).set(1.0F);

        /*
         * ========================================================
         * 鬼域层级
         * ========================================================
         */

        shader.getUniform(
                "GhostEyeDomainLayer"
        ).set(
                (float) ghostEyeDomain.getLayer()
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