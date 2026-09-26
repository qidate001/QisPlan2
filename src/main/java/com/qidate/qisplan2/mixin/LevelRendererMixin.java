package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainVisionSystem;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainMatrices;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostDomainRenderManager;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(
            method = "renderLevel",
            at = @At("HEAD")
    )
    private void qisplan2$captureMatrices(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        GhostDomainMatrices.captureModelViewMatrix(
                new Matrix4f(frustumMatrix)
        );

        GhostDomainMatrices.captureProjectionMatrix(
                new Matrix4f(projectionMatrix)
        );
    }


    @Inject(
            method = "renderLevel",
            at = @At("TAIL")
    )
    private void qisplan2$renderGhostEye(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        GhostDomainRenderManager.render();
    }


    /*
     * ============================================================
     * 鬼域视觉描边颜色
     * ============================================================
     *
     * LevelRenderer 原版流程：
     *
     * entity.getTeamColor()
     * ↓
     * ARGB32.red / green / blue
     * ↓
     * OutlineBufferSource.setColor()
     *
     * 这里仅替换 renderLevel() 中这一处
     * getTeamColor() 的返回值。
     *
     * 不修改 Entity.getTeamColor() 本身，
     * 避免影响 Minecraft 其他使用队伍颜色的逻辑。
     */
    @Redirect(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"
            )
    )
    private int qisplan2$redirectGhostDomainOutlineColor(
            Entity entity
    ) {
        /*
         * 鬼域视觉系统中存在颜色记录。
         *
         * 直接使用鬼域提供的颜色。
         */
        Integer color =
                ClientGhostDomainVisionSystem.getColor(
                        entity.getUUID()
                );

        if (color != null) {
            return color;
        }

        /*
         * 不属于鬼域视觉系统：
         * 完全保留 Minecraft 原版队伍颜色。
         */
        return entity.getTeamColor();
    }
}