package com.qidate.qisplan2.entity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class GhostPaintingEntity extends HangingEntity {


    /*
     * ========================================
     * 构造
     * ========================================
     */

    public GhostPaintingEntity(
            EntityType<? extends GhostPaintingEntity> type,
            Level level
    ) {
        super(type, level);
    }

    public GhostPaintingEntity(
            EntityType<? extends GhostPaintingEntity> type,
            Level level,
            BlockPos pos
    ) {
        super(type, level, pos);
    }

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

    @Override
    protected AABB calculateBoundingBox(
            BlockPos pos,
            Direction direction
    ) {

        GhostPaintingVariant variant = getVariant();

        double halfW = variant.width() / 2.0D;
        double halfH = variant.height() / 2.0D;
        double thickness = 0.0625D;

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        return switch (direction.getAxis()) {

            case Z -> new AABB(
                    x - halfW,
                    y - halfH,
                    z - thickness,
                    x + halfW,
                    y + halfH,
                    z + thickness
            );

            case X -> new AABB(
                    x - thickness,
                    y - halfH,
                    z - halfW,
                    x + thickness,
                    y + halfH,
                    z + halfW
            );

            default -> new AABB(
                    x - halfW,
                    y - halfH,
                    z - thickness,
                    x + halfW,
                    y + halfH,
                    z + thickness
            );
        };
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

    public int getWidth() {
        return getVariant().width() * 16;
    }

    public int getHeight() {
        return getVariant().height() * 16;
    }

    @Override
    public void playPlacementSound() {
        // 暂时不播放
    }

    @Override
    public void dropItem(Entity breaker) {
        // 暂时什么都不掉
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
        return getDirection();
    }

    public void setFacing(Direction direction) {

        if (direction.getAxis() == Direction.Axis.Y) {
            direction = Direction.NORTH;
        }

        setDirection(direction);
    }



    /*
     * ========================================
     * 可被创造破坏
     * ========================================
     */

    @Override
    public boolean skipAttackInteraction(
            Entity attacker
    ) {

        if (attacker instanceof Player player
                && player.isCreative()) {

            if (!level().isClientSide()) {
                discard();
            }

            return true;
        }

        return false;
    }

    /*
     * ========================================
     * NBT 保存
     * ========================================
     */

    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {
        // 朝向
        tag.putString(
                NBT_FACING,
                getDirection().getName()
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
    public void readAdditionalSaveData(CompoundTag tag) {

        if (tag.contains(NBT_FACING)) {
            try {
                setDirection(
                        Direction.valueOf(
                                tag.getString(NBT_FACING).toUpperCase()
                        )
                );
            } catch (IllegalArgumentException ignored) {
                setDirection(Direction.NORTH);
            }
        }

        if (tag.contains(NBT_PAINTING)) {
            try {
                setPaintingId(
                        ResourceLocation.parse(
                                tag.getString(NBT_PAINTING)
                        )
                );
            } catch (Exception ignored) {
                setPaintingId(
                        GhostPaintingVariants.LANDSCAPE
                );
            }
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