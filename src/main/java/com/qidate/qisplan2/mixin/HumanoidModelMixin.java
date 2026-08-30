package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.client.renderer.GhostUmbrellaPlayerRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends LivingEntity> {

    @Inject(
            method = "setupAnim",
            at = @At("RETURN")
    )
    private void qisplan2$afterSetupAnim(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }

        GhostUmbrellaPlayerRenderer.afterSetupAnim(
                player,
                (HumanoidModel<?>) (Object) this
        );
    }
}