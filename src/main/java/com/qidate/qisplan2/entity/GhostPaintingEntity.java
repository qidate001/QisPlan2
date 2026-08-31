package com.qidate.qisplan2.entity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class GhostPaintingEntity extends Entity {

    /*
     * ========================================
     * 画面尺寸
     * ========================================
     */

    public static final float WIDTH = 13.0F;

    public static final float HEIGHT = 7.0F;


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

        tag.putString(
                NBT_FACING,
                facing.getName()
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
    }


    /*
     * ========================================
     * 同步数据
     * ========================================
     */

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) { }
}