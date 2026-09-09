package com.qidate.qisplan2.client.domain;

import com.qidate.qisplan2.ghost.domain.CylinderDomainShape;
import com.qidate.qisplan2.ghost.domain.GhostDomainShape;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class ClientGhostDomain {

    private final UUID id;
    private final UUID sourceUUID;
    private final ResourceLocation domainType;
    private final ResourceLocation dimension;

    private double x;
    private double y;
    private double z;

    private final GhostDomainShape shape;

    public ClientGhostDomain(
            UUID id,
            UUID sourceUUID,
            ResourceLocation domainType,
            ResourceLocation dimension,
            double x,
            double y,
            double z,
            double radius
    ) {
        this.id = id;
        this.sourceUUID = sourceUUID;
        this.domainType = domainType;
        this.dimension = dimension;

        this.x = x;
        this.y = y;
        this.z = z;

        this.shape = new CylinderDomainShape(radius);
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

    public void setPosition(
            double x,
            double y,
            double z
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
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

    public GhostDomainShape getShape() {
        return shape;
    }
}