package com.qidate.qisplan2.ghost.domain.shape;

import com.qidate.qisplan2.ghost.domain.GhostDomainShape;
import net.minecraft.world.phys.Vec3;

public final class CylinderDomainShape
        implements GhostDomainShape {

    private double radius;

    public CylinderDomainShape(
            double radius
    ) {

        this.radius = radius;
    }

    @Override
    public boolean contains(
            double centerX,
            double centerY,
            double centerZ,
            double x,
            double y,
            double z
    ) {

        double dx = x - centerX;
        double dz = z - centerZ;

        return dx * dx + dz * dz
                <= radius * radius;
    }

    /** 使用线段到球心／轴线的最近点求交，快速移动也不能从两侧端点都在域外的位置穿透。 */
    @Override public boolean intersectsSegment(double cx,double cy,double cz,Vec3 from,Vec3 to) {
        Vec3 a=new Vec3(from.x-cx,0,from.z-cz);
        Vec3 delta=new Vec3(to.x-from.x,0,to.z-from.z);
        double length=delta.lengthSqr();double t=length<1.0E-12?0:Math.clamp(-a.dot(delta)/length,0,1);
        return a.add(delta.scale(t)).lengthSqr()<radius*radius;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }
}