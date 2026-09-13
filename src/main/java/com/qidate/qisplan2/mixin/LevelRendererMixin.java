package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.client.GhostDomainAtmosphereClient;
import com.qidate.qisplan2.client.domain.ClientGhostDomain;
import com.qidate.qisplan2.client.domain.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.ability.ghosteye.GhostEyeAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
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

        return GhostDomainAtmosphereClient.applySkyColor(
                original,
                cameraPos.x,
                cameraPos.y,
                cameraPos.z
        );
    }



    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/dimension/DimensionSpecialEffects;getSunriseColor(FF)[F"
            )
    )
    private float[] qisplan2$modifySunriseColor(
            DimensionSpecialEffects effects,
            float timeOfDay,
            float partialTick
    ) {
        ClientGhostDomain domain =
                ClientGhostDomainManager.getEffectiveDomain(
                        Minecraft.getInstance()
                                .gameRenderer
                                .getMainCamera()
                                .getPosition()
                                .x,
                        Minecraft.getInstance()
                                .gameRenderer
                                .getMainCamera()
                                .getPosition()
                                .y,
                        Minecraft.getInstance()
                                .gameRenderer
                                .getMainCamera()
                                .getPosition()
                                .z
                );

        if (domain != null
                && GhostEyeAbility.ID.equals(domain.getDomainType())
                && domain.getLayer() == 1) {

            // 鬼眼领域内：彻底取消日出/日落渐变层
            return null;
        }

        return effects.getSunriseColor(
                timeOfDay,
                partialTick
        );
    }
}