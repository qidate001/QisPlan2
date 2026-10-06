package com.qidate.qisplan2.ghost.domain.client.renderer.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomain;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainMatrices;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainRenderEffect;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainRenderEffectRegistry;
import com.qidate.qisplan2.ghost.domain.type.umbrella.GhostUmbrellaDomainController;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class GhostUmbrellaRenderEffect
        implements GhostDomainRenderEffect {

    private static final GhostUmbrellaRenderEffect INSTANCE =
            new GhostUmbrellaRenderEffect();

    private GhostUmbrellaRenderEffect() {
    }

    public static void register() {

        GhostDomainRenderEffectRegistry.register(
                INSTANCE
        );
    }

    public static GhostUmbrellaRenderEffect getInstance() {
        return INSTANCE;
    }

    @Override
    public ResourceLocation shaderId() {

        return GhostUmbrellaDomainController.DOMAIN_TYPE;
    }

    @Override
    public boolean shouldRender(
            Minecraft minecraft
    ) {

        if (minecraft.level == null) {
            return false;
        }

        return !getDomains(
                minecraft
        ).isEmpty();
    }

    @Override
    public void setupUniforms(
            ShaderInstance shader,
            Minecraft minecraft,
            ClientGhostDomain domain
    ) {

        if (domain == null) {
            return;
        }

        /*
         * ========================================================
         * Projection / ModelView
         * ========================================================
         */

        shader.getUniform(
                "GhostUmbrellaProjMat"
        ).set(
                RenderSystem.getProjectionMatrix()
        );

        shader.getUniform(
                "GhostUmbrellaModelViewMat"
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
                "GhostUmbrellaCameraPos"
        ).set(
                (float) camera.getPosition().x,
                (float) camera.getPosition().y,
                (float) camera.getPosition().z
        );

        /*
         * ========================================================
         * 圆柱鬼域中心
         *
         * 圆柱只需要 X / Z。
         * Y 可以保留为 0。
         * ========================================================
         */

        shader.getUniform(
                "GhostUmbrellaDomainCenter"
        ).set(
                (float) domain.getX(),
                0.0F,
                (float) domain.getZ()
        );

        /*
         * ========================================================
         * 圆柱半径
         * ========================================================
         */

        shader.getUniform(
                "GhostUmbrellaDomainRadius"
        ).set(
                (float) domain.getRenderRadius()
        );

        /*
         * ========================================================
         * 鬼域启用状态
         * ========================================================
         */

        shader.getUniform(
                "GhostUmbrellaDomainActive"
        ).set(
                1.0F
        );

        /*
         * ========================================================
         * 鬼域层级
         * ========================================================
         */

        shader.getUniform(
                "GhostUmbrellaDomainLayer"
        ).set(
                (float) domain.getLayer()
        );

        /*
         * ========================================================
         * 鬼域来源所在的灵异隔绝 Region
         *
         * 0            = 普通世界
         * 1 / 256      = Region Index 0
         * 2 / 256      = Region Index 1
         * 3 / 256      = Region Index 2
         * ...
         *
         * 必须与 ghost_isolation_region.fsh
         * 使用完全相同的编码。
         * ========================================================
         */

        UUID sourceRegionUUID =
                domain.getSourceRegionUUID();

        if (sourceRegionUUID == null) {

            shader.getUniform(
                    "GhostUmbrellaSourceRegion"
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
                    "GhostUmbrellaSourceRegion"
            ).set(
                    encodedSourceRegion
            );
        }
    }
}