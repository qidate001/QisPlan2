package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPushMixin {

    @Inject(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void qisplan2$ghostLayerPush(
            Entity other,
            CallbackInfo ci
    ) {
        Entity self = (Entity) (Object) this;

        if (!GhostLayerHandler.canSee(
                self,
                other
        )) {
            ci.cancel();
        }
    }
}