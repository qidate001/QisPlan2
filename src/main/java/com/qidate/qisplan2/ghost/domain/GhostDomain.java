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

    /**
     * 鬼域强度。
     */
    private final double strength;

    /**
     * 鬼域层数，范围 1～10。
     */
    private final int layer;

    private double x;
    private double y;
    private double z;

    private final GhostDomainShape shape;

    private final GhostDomainUpdateMode updateMode;

    private final double updateDistance;

    private final GhostDomainBehavior behavior;


    public GhostDomain(
            UUID id,
            UUID sourceUUID,
            ResourceLocation type,
            double strength,
            int layer,
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            GhostDomainShape shape,
            GhostDomainUpdateMode updateMode,
            double updateDistance,
            GhostDomainBehavior behavior
    ) {
        if (strength < 0.0D) {
            throw new IllegalArgumentException(
                    "GhostDomain strength cannot be negative."
            );
        }

        if (layer < 1 || layer > 10) {
            throw new IllegalArgumentException(
                    "GhostDomain layer must be between 1 and 10."
            );
        }

        this.id = id;
        this.sourceUUID = sourceUUID;
        this.type = type;
        this.strength = strength;
        this.layer = layer;
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

    public double getStrength() {
        return strength;
    }

    public int getLayer() {
        return layer;
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