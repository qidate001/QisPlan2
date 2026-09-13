package com.qidate.qisplan2.client.domain;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.domain.CylinderDomainShape;
import com.qidate.qisplan2.ghost.domain.GhostDomainShape;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class ClientGhostDomain {

    private final UUID id;
    private final UUID sourceUUID;
    private final ResourceLocation domainType;
    private final ResourceLocation dimension;

    // 当前客户端实际使用的位置
    private double x;
    private double y;
    private double z;

    // 服务端同步过来的目标位置
    private double targetX;
    private double targetY;
    private double targetZ;

    private final GhostDomainShape shape;
    private double strength;
    private int layer;
    private double radius;

    public ClientGhostDomain(
            UUID id,
            UUID sourceUUID,
            ResourceLocation domainType,
            ResourceLocation dimension,
            double x,
            double y,
            double z,
            double strength,
            int layer,
            double radius
    ) {
        this.id = id;
        this.sourceUUID = sourceUUID;
        this.domainType = domainType;
        this.dimension = dimension;

        // 初始位置直接使用服务器位置
        this.x = x;
        this.y = y;
        this.z = z;

        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;

        this.strength = strength;
        this.layer = layer;
        this.shape = new CylinderDomainShape(radius);
        this.radius = radius;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceUUID() {
        return sourceUUID;
    }

    public ResourceLocation getDomainType() {
        return domainType;
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public GhostDomainShape getShape() {
        return shape;
    }

    public double getStrength() {
        return strength;
    }

    public int getLayer() {
        return layer;
    }

    public double getRadius() {
        return radius;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public void setRadius(double radius) {

        this.radius = radius;

        if (shape instanceof CylinderDomainShape cylinder) {
            cylinder.setRadius(radius);
        }
    }

    /**
     * 设置服务端同步过来的目标位置。
     *
     * 不直接修改当前渲染位置，
     * 当前渲染位置会在 tick() 中逐渐追上目标位置。
     */
    public void setPosition(
            double x,
            double y,
            double z
    ) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    /**
     * 客户端平滑更新鬼域位置。
     */
    public void tick() {

        moveAxis();
    }

    private void moveAxis() {

        double speed = 0.9D; // 每tick最多移动0.9格

        x = moveTowards(x, targetX, speed);
        y = moveTowards(y, targetY, speed);
        z = moveTowards(z, targetZ, speed);
    }

    private static double moveTowards(
            double current,
            double target,
            double maxStep
    ) {

        double delta = target - current;

        if (Math.abs(delta) <= maxStep) {
            return target;
        }

        return current + Math.copySign(maxStep, delta);
    }

    public boolean contains(
            double x,
            double y,
            double z
    ) {
        return shape.contains(
                this.x,
                this.y,
                this.z,
                x,
                y,
                z
        );
    }
}