package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCollisionMixin {

    @Inject(
            method = "canCollideWith",
            at = @At("HEAD"),
            cancellable = true
    )
    private void qisplan2$ghostLayerCollision(
            Entity other,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Entity self = (Entity) (Object) this;

        if (!GhostLayerHandler.canSee(self, other)) {
            cir.setReturnValue(false);
        }
    }
}