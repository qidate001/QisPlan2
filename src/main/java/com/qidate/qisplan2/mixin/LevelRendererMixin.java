package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.client.GhostRainAtmosphereClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 qisplan2$modifySkyColor(
            ClientLevel level,
            Vec3 cameraPos,
            float partialTick
    ) {

        Vec3 original =
                level.getSkyColor(
                        cameraPos,
                        partialTick
                );

        return GhostRainAtmosphereClient.applySkyDarkness(
                original,
                cameraPos.x,
                cameraPos.z
        );
    }
}