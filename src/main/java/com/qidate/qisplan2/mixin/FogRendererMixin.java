package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.client.domain.ClientGhostDomain;
import com.qidate.qisplan2.client.domain.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.ability.ghosteye.GhostEyeAbility;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Shadow
    private static float fogRed;

    @Shadow
    private static float fogGreen;

    @Shadow
    private static float fogBlue;

    @Inject(
            method = "setupColor",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;clearColor(FFFF)V",
                    ordinal = 1
            )
    )
    private static void qisplan2$modifyFogColor(
            Camera camera,
            float partialTicks,
            ClientLevel level,
            int renderDistanceChunks,
            float bossColorModifier,
            CallbackInfo ci
    ) {

        ClientGhostDomain domain =
                ClientGhostDomainManager.getEffectiveDomain(
                        camera.getPosition().x,
                        camera.getPosition().y,
                        camera.getPosition().z
                );

        if (domain == null) {
            return;
        }

        if (!GhostEyeAbility.ID.equals(
                domain.getDomainType()
        )) {
            return;
        }

        if (domain.getLayer() != 1) {
            return;
        }

        // 测试：鬼眼第一层将远处雾气染成纯红色
        fogRed = 1.0F;
        fogGreen = 0.0F;
        fogBlue = 0.0F;
    }
}