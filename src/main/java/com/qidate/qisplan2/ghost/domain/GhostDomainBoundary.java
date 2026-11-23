package com.qidate.qisplan2.ghost.domain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;

/** 世界运动边界，不依赖任何模块名称。出生放置不算入侵；跨界权限由域的适配行为解释。 */
public final class GhostDomainBoundary {
    private GhostDomainBoundary() {}
    /** 返回是否阻止本次位置变化；相同位置、客户端及没有鬼域时零扫描。 */
    public static boolean blocked(Entity entity,Vec3 to) {
        if(!(entity.level() instanceof ServerLevel level) || entity.tickCount==0 || !entity.isAddedToLevel()
                || entity.isRemoved() || entity.isSpectator() || !GhostDomainEntityPolicy.shouldTrack(entity)
                || entity.position().distanceToSqr(to)<1.0E-12 || GhostDomainManager.get(level).getDomains().isEmpty())return false;
        Vec3 from=entity.position();
        for(var domain:GhostDomainEntityTracker.get(level).movementCandidates(from,to)) {
            if(entity.getUUID().equals(domain.getSourceUUID()) || !domain.getBehavior().sourcePresent(level,domain))continue;
            boolean inside=domain.contains(from.x,from.y,from.z),after=domain.contains(to.x,to.y,to.z);
            boolean through=!inside && !after && domain.getShape().intersectsSegment(domain.getX(),domain.getY(),domain.getZ(),from,to);
            if(inside==after&&!through)continue;
            if(denied(inside,after,through,domain.getBehavior().permitsCrossing(level,domain,entity,true),
                    domain.getBehavior().permitsCrossing(level,domain,entity,false)))return true;
        }
        return false;
    }
    /** 原版传送门提供真实目的地后，在换维度、移除旧实体之前校验进入权限。 */
    public static boolean blocksArrival(Entity entity,DimensionTransition transition) {
        if(entity.isSpectator()||!GhostDomainEntityPolicy.shouldTrack(entity))return false;
        if(entity.level()==transition.newLevel())return blocked(entity,transition.pos());
        var level=transition.newLevel();var pos=transition.pos();
        for(var domain:GhostDomainEntityTracker.get(level).movementCandidates(pos,pos)) {
            if(entity.getUUID().equals(domain.getSourceUUID())||!domain.contains(pos.x,pos.y,pos.z)
                    ||!domain.getBehavior().sourcePresent(level,domain))continue;
            if(!domain.getBehavior().permitsCrossing(level,domain,entity,true))return true;
        }
        return false;
    }
    /** 进出各自独立；域内移动不受进出开关影响，穿越整个域必须同时满足两侧权限。 */
    public static boolean denied(boolean before,boolean after,boolean through,boolean enter,boolean leave) {
        return !before&&(after||through)&&!enter || (before&&!after||through)&&!leave;
    }
    /** 回传服务器的原位置，避免客户端认为自己已经穿过边界。 */
    public static void correct(Entity entity) {
        entity.setDeltaMovement(Vec3.ZERO);
        if(entity instanceof ServerPlayer player)player.connection.teleport(entity.getX(),entity.getY(),entity.getZ(),entity.getYRot(),entity.getXRot());
    }
}
