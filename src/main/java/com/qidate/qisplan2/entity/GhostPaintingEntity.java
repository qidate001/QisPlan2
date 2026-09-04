package com.qidate.qisplan2.entity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

public class GhostPaintingEntity extends Entity {

    /*
     * ========================================
     * 画面尺寸
     * ========================================
     */

    private static final EntityDataAccessor<String> PAINTING =
            SynchedEntityData.defineId(
                    GhostPaintingEntity.class,
                    EntityDataSerializers.STRING
            );

    public GhostPaintingVariant getVariant() {

        return GhostPaintingVariants.get(
                getPaintingId()
        );
    }

    public ResourceLocation getPaintingId() {

        return ResourceLocation.parse(
                entityData.get(PAINTING)
        );
    }

    public void setPaintingId(
            ResourceLocation id
    ) {

        entityData.set(
                PAINTING,
                id.toString()
        );

        refreshDimensions();
    }


    /*
     * ========================================
     * 墙面朝向
     * ========================================
     */

    private Direction facing =
            Direction.NORTH;


    /*
     * ========================================
     * NBT
     * ========================================
     */

    private static final String NBT_FACING =
            "Facing";

    private static final String NBT_PAINTING =
            "Painting";


    /*
     * ========================================
     * 构造
     * ========================================
     */

    public GhostPaintingEntity(
            EntityType<? extends GhostPaintingEntity> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );

        /*
         * 不受重力。
         */
        setNoGravity(true);

        /*
         * 不参与实体物理。
         */
        noPhysics = true;
    }


    /*
     * ========================================
     * Tick
     * ========================================
     */

    @Override
    public void tick() {

        super.tick();

        /*
         * 第一阶段暂时什么都不做。
         */
    }


    /*
     * ========================================
     * 实体尺寸
     * ========================================
     */

    @Override
    public EntityDimensions getDimensions(Pose pose) {

        GhostPaintingVariant variant =
                getVariant();

        return EntityDimensions.fixed(
                variant.width(),
                variant.height()
        );
    }


    /*
     * ========================================
     * 朝向
     * ========================================
     */

    public Direction getFacing() {

        return facing;
    }


    public void setFacing(
            Direction direction
    ) {

        /*
         * 不允许上下方向。
         */
        if (direction.getAxis()
                == Direction.Axis.Y) {

            direction =
                    Direction.NORTH;
        }

        facing =
                direction;

        /*
         * 同步实体旋转。
         */
        setYRot(
                direction.toYRot()
        );

        yRotO =
                getYRot();
    }


    /*
     * ========================================
     * NBT 保存
     * ========================================
     */

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        // 朝向
        tag.putString(
                NBT_FACING,
                facing.getName()
        );

        // 画ID
        tag.putString(
                NBT_PAINTING,
                getPaintingId().toString()
        );
    }


    /*
     * ========================================
     * NBT 读取
     * ========================================
     */

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {

        if (!tag.contains(
                NBT_FACING
        )) {

            return;
        }

        // 朝向
        try {

            setFacing(
                    Direction.valueOf(
                            tag.getString(
                                    NBT_FACING
                            ).toUpperCase()
                    )
            );

        } catch (IllegalArgumentException ignored) {

            setFacing(
                    Direction.NORTH
            );
        }

        // 画ID
        if (tag.contains(NBT_PAINTING)) {

            setPaintingId(
                    ResourceLocation.parse(
                            tag.getString(
                                    NBT_PAINTING
                            )
                    )
            );

            // 刷新尺寸
            refreshDimensions();
        }
    }


    /*
     * ========================================
     * 同步数据
     * ========================================
     */

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {

        builder.define(
                PAINTING,
                GhostPaintingVariants.LANDSCAPE.toString()
        );
    }
}