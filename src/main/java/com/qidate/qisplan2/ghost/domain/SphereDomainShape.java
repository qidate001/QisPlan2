package com.qidate.qisplan2.ghost.domain;

public final class SphereDomainShape
        implements GhostDomainShape {

    private double radius;

    public SphereDomainShape(
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
        double dy = y - centerY;
        double dz = z - centerZ;

        return dx * dx
                + dy * dy
                + dz * dz
                <= radius * radius;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }
}