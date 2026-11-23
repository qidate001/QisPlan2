package com.qidate.qisplan2.ghost.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface GhostDomainShape {
    /** 路径是否穿过形状内部；自定义形状可覆盖为解析求交，默认逐段采样。 */
    default boolean intersectsSegment(double cx,double cy,double cz,Vec3 from,Vec3 to) {
        int steps=(int)Math.min(4096,Math.ceil(from.distanceTo(to)*4));
        for(int i=0;i<=steps;i++) {var p=from.lerp(to,steps==0?0:(double)i/steps);if(contains(cx,cy,cz,p.x,p.y,p.z))return true;}
        return false;
    }


    boolean contains(
            double centerX,
            double centerY,
            double centerZ,
            double x,
            double y,
            double z
    );

    default boolean contains(
            double centerX,
            double centerY,
            double centerZ,
            BlockPos pos
    ) {

        return contains(
                centerX,
                centerY,
                centerZ,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );
    }
}