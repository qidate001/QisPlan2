package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.renderer.GhostEyeRenderHandler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    private void qisplan2$captureModelViewMatrix(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        GhostEyeRenderHandler.captureModelViewMatrix(
                new Matrix4f(frustumMatrix)
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
        GhostEyeRenderHandler.render();
    }
}