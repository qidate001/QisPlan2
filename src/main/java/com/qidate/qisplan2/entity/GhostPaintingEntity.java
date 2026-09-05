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

        recalculateBoundingBox();
    }

    @Override
    protected AABB calculateBoundingBox(
            BlockPos pos,
            Direction direction
    ) {
        GhostPaintingVariant variant = getVariant();

        double width = variant.width();
        double height = variant.height();

        /*
         * HangingEntity 的挂载点。
         *
         * pos 是画的挂载参考位置，
         * 不是需要再沿横向平移 halfWidth 的中心点。
         */
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;

        /*
         * 画面的横向方向。
         */
        Direction horizontal =
                direction.getClockWise();

        /*
         * 画面的半尺寸。
         */
        double halfWidth =
                width / 2.0D;

        double halfHeight =
                height / 2.0D;

        /*
         * 贴墙厚度。
         *
         * 不需要真的占半格，
         * 这里只需要一个非常薄的碰撞体。
         */
        double thickness = 0.0625D;

        /*
         * ========================================
         * 横向
         * ========================================
         */

        double minX;
        double maxX;

        double minZ;
        double maxZ;

        if (horizontal.getAxis() == Direction.Axis.X) {

            /*
             * NORTH / SOUTH
             *
             * 画面横向 = X
             */
            minX =
                    centerX - halfWidth;

            maxX =
                    centerX + halfWidth;

            minZ =
                    centerZ - thickness;

            maxZ =
                    centerZ + thickness;

        } else {

            /*
             * EAST / WEST
             *
             * 画面横向 = Z
             */
            minX =
                    centerX - thickness;

            maxX =
                    centerX + thickness;

            minZ =
                    centerZ - halfWidth;

            maxZ =
                    centerZ + halfWidth;
        }

        /*
         * ========================================
         * Y
         * ========================================
         */

        double minY =
                centerY - halfHeight;

        double maxY =
                centerY + halfHeight;

        return new AABB(
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
        );
    }


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

    @Override
    public void onSyncedDataUpdated(
            EntityDataAccessor<?> key
    ) {
        super.onSyncedDataUpdated(key);

        /*
         * 方向 / Variant 从服务器同步到客户端后，
         * 重新计算 HangingEntity 的 AABB。
         */
        recalculateBoundingBox();
    }
}