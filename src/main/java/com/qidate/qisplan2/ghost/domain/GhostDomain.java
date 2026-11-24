package com.qidate.qisplan2.ghost.domain;

import net.minecraft.world.phys.Vec3;
import com.qidate.qisplan2.ghost.domain.shape.CylinderDomainShape;
import com.qidate.qisplan2.ghost.domain.shape.SphereDomainShape;
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
    private double strength;

    /**
     * 鬼域层数，范围 1～10。
     */
    private int layer;

    /**
     * 鬼域范围
     */
    private double radius;
    private double conflictRadius;
    private static long changes;
    /** 任意原始参数变化时递增；稳定状态不重新求解对抗。 */
    public static long changeVersion() { return changes; }
    /** 声明的原始范围，网络与重新求解必须读取此值。 */
    public double getBaseRadius() { return radius; }
    /** 等级独立于是否分层；旧 getLayer 保留为等级读取兼容入口。 */
    public int getGrade() { return layer; }
    /** 非分层域即使等级很高，也只有一个可占据空间层。 */
    public int maxOccupiedLayer() { return behavior.supportsLayering() ? layer : 1; }
    /** 对抗结果只更新有效形状，不污染原始范围或触发下一轮缩小。 */
    public void applyConflictRadius(double value) {
        conflictRadius=value;
        if(shape instanceof CylinderDomainShape cylinder)cylinder.setRadius(value);
        else if(shape instanceof SphereDomainShape sphere)sphere.setRadius(value);
    }
    /** 提供不包含宿主类型的不可变几何快照。 */
    public GhostDomainConflict.Node conflictNode() {
        return new GhostDomainConflict.Node(id,new Vec3(x,y,z),radius,strength,layer,
                shape instanceof SphereDomainShape ? 1 : 0);
    }

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
            double radius,
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            GhostDomainShape shape,
            GhostDomainUpdateMode updateMode,
            double updateDistance,
            GhostDomainBehavior behavior
    ) {
        if (!Double.isFinite(strength) || strength < 0.0D) {
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
        if(this.strength!=strength)changes++;
        this.strength = strength;
        if(this.layer!=layer)changes++;
        this.layer = layer;
        this.radius = radius;
        this.conflictRadius = radius;
        changes++;
        this.dimension = dimension;
        if(this.x!=x||this.y!=y||this.z!=z)changes++;
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

    public double getRadius() {
        return conflictRadius;
    }

    public void setStrength(double strength) {

        if (!Double.isFinite(strength) || strength < 0.0D) {
            throw new IllegalArgumentException(
                    "GhostDomain strength cannot be negative."
            );
        }

        if(this.strength!=strength)changes++;
        this.strength = strength;
    }

    public void setLayer(int layer) {

        if (layer < 1 || layer > 10) {
            throw new IllegalArgumentException(
                    "GhostDomain layer must be between 1 and 10."
            );
        }

        if(this.layer!=layer)changes++;
        this.layer = layer;
    }

    public void setRadius(double radius) {

        if (radius < 0.0D) {
            throw new IllegalArgumentException(
                    "GhostDomain radius cannot be negative."
            );
        }

        this.radius = radius;
        this.conflictRadius = radius;
        changes++;

        if (shape instanceof CylinderDomainShape cylinder) {
            cylinder.setRadius(radius);
        } else if (shape instanceof SphereDomainShape sphere) {
            sphere.setRadius(radius);
        }
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

        if(this.x!=x||this.y!=y||this.z!=z)changes++;
        this.x = x;
        this.y = y;
        this.z = z;
    }


    public boolean contains(
            double x,
            double y,
            double z
    ) {

        return conflictRadius>0 && shape.contains(
                this.x,
                this.y,
                this.z,
                x,
                y,
                z
        );
    }
}