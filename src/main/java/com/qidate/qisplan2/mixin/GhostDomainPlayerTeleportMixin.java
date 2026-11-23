package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.ghost.domain.GhostDomainBoundary;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Set;

/** 在玩家传送握手开始前拒绝非法跨界，避免客户端收到一个服务器已经拒绝的位置。 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class GhostDomainPlayerTeleportMixin {
    @Shadow public ServerPlayer player;
    /** 此处坐标是绝对目的地，relativeSet 仅用于编码发往客户端的差值。 */
    @Inject(method="teleport(DDDFFLjava/util/Set;)V",at=@At("HEAD"),cancellable=true)
    private void qisplan2$rejectDomainTeleport(double x,double y,double z,float yaw,float pitch,Set<RelativeMovement> flags,CallbackInfo ci){
        if(GhostDomainBoundary.blocked(player,new Vec3(x,y,z))){ci.cancel();GhostDomainBoundary.correct(player);}
    }
}
