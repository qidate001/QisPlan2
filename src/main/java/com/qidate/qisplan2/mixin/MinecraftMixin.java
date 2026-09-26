package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainVisionSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(
            method = "shouldEntityAppearGlowing",
            at = @At("RETURN"),
            cancellable = true
    )
    private void qisplan2$ghostDomainVision(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (ClientGhostDomainVisionSystem.shouldOutline(
                entity.getUUID()
        )) {
            cir.setReturnValue(true);
        }
    }
}