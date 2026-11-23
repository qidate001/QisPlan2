package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.ghost.domain.GhostDomainBoundary;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 玩家重写了换维度方法，因此同时覆盖两种入口；新实体出生和读档不经过此入侵检查。 */
@Mixin({Entity.class,ServerPlayer.class})
public abstract class GhostDomainDimensionMixin {
    /** 拒绝时原实体仍留在旧维度，不生成半完成的传送副本。 */
    @Inject(method="changeDimension",at=@At("HEAD"),cancellable=true)
    private void qisplan2$checkDomainArrival(DimensionTransition transition,CallbackInfoReturnable<Entity> cir){
        if(GhostDomainBoundary.blocksArrival((Entity)(Object)this,transition))cir.setReturnValue(null);
    }
}
