package com.qidate.qisplan2.ghost.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class GhostDomain {

    /*
     * ============================================================
     * 基本信息
     * ============================================================
     */

    private final UUID id;

    /*
     * 创建这个鬼域的来源。
     *
     * 例如：
     *
     * 鬼伞玩家 UUID
     * 鬼湖实体 UUID
     * 鬼庙 UUID
     *
     * 可以为空。
     */
    private final UUID sourceUUID;


    /*
     * 所属维度。
     */
    private final ResourceKey<net.minecraft.world.level.Level> dimension;


    /*
     * ============================================================
     * 空间
     * ============================================================
     */

    private double x;
    private double y;
    private double z;

    private final GhostDomainShape shape;


    /*
     * ============================================================
     * 构造
     * ============================================================
     */

    public GhostDomain(
            UUID id,
            UUID sourceUUID,
            ServerLevel level,
            double x,
            double y,
            double z,
            GhostDomainShape shape
    ) {

        this.id = id;
        this.sourceUUID = sourceUUID;
        this.dimension = level.dimension();

        this.x = x;
        this.y = y;
        this.z = z;

        this.shape = shape;
    }


    /*
     * ============================================================
     * Getter
     * ============================================================
     */

    public UUID getId() {
        return id;
    }

    public UUID getSourceUUID() {
        return sourceUUID;
    }

    public ResourceKey<net.minecraft.world.level.Level>
    getDimension() {

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

    public double getRadius() {
        return radius;
    }


    /*
     * ============================================================
     * 移动
     * ============================================================
     */

    public void setPosition(
            double x,
            double y,
            double z
    ) {

        this.x = x;
        this.y = y;
        this.z = z;
    }


    /*
     * ============================================================
     * 空间判定
     * ============================================================
     */

    public boolean contains(
            ServerLevel level,
            Entity entity
    ) {

        if (level.dimension() != dimension) {
            return false;
        }

        if (entity.level().dimension() != dimension) {
            return false;
        }

        return contains(
                entity.getX(),
                entity.getY(),
                entity.getZ()
        );
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


    public boolean contains(
            BlockPos pos
    ) {

        return contains(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );
    }
}