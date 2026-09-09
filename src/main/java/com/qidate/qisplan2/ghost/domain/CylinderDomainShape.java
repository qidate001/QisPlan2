package com.qidate.qisplan2.ghost.domain;

public final class CylinderDomainShape
        implements GhostDomainShape {

    private final double radius;

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

    public double getRadius() {
        return radius;
    }
}