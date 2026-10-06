package com.qidate.qisplan2.ghost.domain.client.renderer.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomain;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainMatrices;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainRenderEffect;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainRenderEffectRegistry;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationManager;
import com.qidate.qisplan2.ghost.possession.ability.ghosteye.GhostEyeAbility;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

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

        return !getDomains(minecraft).isEmpty();
    }

    /**
     * 获取当前客户端所有 GhostEye 鬼域。
     *
     * <p>
     * 不再只获取第一个 GhostEye Domain。
     * </p>
     */
    @Override
    public Collection<ClientGhostDomain> getDomains(
            Minecraft minecraft
    ) {

        if (minecraft.level == null) {
            return Collections.emptyList();
        }

        return ClientGhostDomainManager.getDomains()
                .stream()
                .filter(domain ->
                        domain.getDimension().equals(
                                minecraft.level
                                        .dimension()
                                        .location()
                        )
                )
                .filter(domain ->
                        GhostEyeAbility.ID.equals(
                                domain.getDomainType()
                        )
                )
                .toList();
    }

    /**
     * GhostEye 不使用传统的
     * “Stencil = 0 才允许渲染”规则。
     *
     * <p>
     * GhostEye 使用 Region Identity
     * 判断当前像素是否属于 Source 所在的空间。
     * </p>
     */
    @Override
    public boolean useIsolationStencil() {
        return false;
    }

    @Override
    public void setupUniforms(
            ShaderInstance shader,
            Minecraft minecraft,
            ClientGhostDomain ghostEyeDomain
    ) {

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
                (float) ghostEyeDomain.getRenderRadius()
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

        /*
         * ========================================================
         * 设置 GhostEye Source 所在的空间身份。
         * ========================================================
         *
         * GhostEyeRegionIdentity Shader 中的空间编号约定：
         *
         *     0
         *         = 开放空间
         *
         *     1
         *         = 客户端 GPU Region Index 0
         *
         *     2
         *         = 客户端 GPU Region Index 1
         *
         *     ...
         *
         * 因此这里不能直接把
         * ClientGhostIsolationManager 的 GPU Index
         * 传给 Shader。
         *
         * 必须进行：
         *
         *     GPU Index + 1
         *
         * ========================================================
         */

        UUID sourceRegionUUID =
                ghostEyeDomain.getSourceRegionUUID();

        if (sourceRegionUUID == null) {

            shader.getUniform("GhostEyeSourceRegion").set(
                    0.0F
            );

        } else {

            int regionIndex =
                    ClientGhostIsolationManager.getRegionIndex(
                            sourceRegionUUID
                    );

            float encodedSourceRegion =
                    (float) (regionIndex + 1) / 256.0F;

            shader.getUniform("GhostEyeSourceRegion").set(
                    encodedSourceRegion
            );
        }
    }
}