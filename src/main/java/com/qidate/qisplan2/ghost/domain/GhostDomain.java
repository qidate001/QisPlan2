package com.qidate.qisplan2.ghost.domain;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class GhostDomain {

    private final UUID id;

    private final UUID sourceUUID;

    private final ResourceLocation type;

    private final ResourceKey<Level> dimension;

    private double x;
    private double y;
    private double z;

    private final GhostDomainShape shape;
    private final GhostDomainUpdateMode updateMode;
    private final GhostDomainBehavior behavior;
    private final double updateDistance;


    public GhostDomain(
            UUID id,
            UUID sourceUUID,
            ResourceLocation type,
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            GhostDomainShape shape,
            GhostDomainUpdateMode updateMode,
            double updateDistance,
            GhostDomainBehavior behavior
    ) {
        this.id = id;
        this.sourceUUID = sourceUUID;
        this.type = type;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.shape = shape;
        this.updateMode = updateMode;
        this.updateDistance = updateDistance;
        this.behavior = behavior;
    }


    public UUID getId() {
        return id;
    }


    public UUID getSourceUUID() {
        return sourceUUID;
    }


    public ResourceLocation getType() {
        return type;
    }


    public ResourceKey<Level> getDimension() {
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

    public GhostDomainUpdateMode getUpdateMode() {
        return updateMode;
    }

    public double getUpdateDistance() {
        return updateDistance;
    }

    public GhostDomainBehavior getBehavior() {
        return behavior;
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
}