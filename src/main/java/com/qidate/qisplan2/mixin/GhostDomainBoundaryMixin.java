package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.ghost.domain.GhostDomainBoundary;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.server.level.ServerLevel;
import com.qidate.qisplan2.ghost.domain.GhostDomainEntityTracker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 拦截实体真正提交的位置，普通移动、击退及同维度瞬移共用同一服务器权限。 */
@Mixin(Entity.class)
public abstract class GhostDomainBoundaryMixin {
    @Unique private Vec3 qisplan2$previousPosition;
    /** 同位置纠正不会再次跨界；拒绝位置提交后通知玩家客户端回到服务器位置。 */
    @Inject(method="setPosRaw(DDD)V",at=@At("HEAD"),cancellable=true)
    private void qisplan2$domainBoundary(double x,double y,double z,CallbackInfo ci) {
        Entity entity=(Entity)(Object)this;
        qisplan2$previousPosition=entity.position();
        if(GhostDomainBoundary.blocked(entity,new Vec3(x,y,z))){ci.cancel();GhostDomainBoundary.correct(entity);}
    }
    /** 同区块进出也要更新有效覆盖；普通域内移动无需重新构建关系。 */
    @Inject(method="setPosRaw(DDD)V",at=@At("TAIL"))
    private void qisplan2$domainMembership(double x,double y,double z,CallbackInfo ci) {
        Entity entity=(Entity)(Object)this;Vec3 from=qisplan2$previousPosition;
        if(from==null || entity.tickCount==0 || !entity.isAddedToLevel() || !(entity.level() instanceof ServerLevel level)
                || from.distanceToSqr(entity.position())<1.0E-12)return;
        var tracker=GhostDomainEntityTracker.get(level);
        for(var domain:tracker.movementCandidates(from,entity.position())) {
            if(domain.contains(from.x,from.y,from.z)!=domain.contains(x,y,z)){tracker.update(entity);break;}
        }
    }
}
