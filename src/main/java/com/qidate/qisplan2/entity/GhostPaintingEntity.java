package com.qidate.qisplan2.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
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

        System.out.println(
                "[GhostPainting DEBUG] constructor"
                        + " pos=" + pos
                        + " entityPos=" + position()
                        + " blockPosition=" + blockPosition()
                        + " direction=" + getDirection()
        );
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

        double centerX =
                pos.getX()
                        + 0.49D
                        - direction.getStepX() * 0.49D;

        double centerY =
                pos.getY()
                        + 0.49D
                        - direction.getStepY() * 0.49D;

        double centerZ =
                pos.getZ()
                        + 0.49D
                        - direction.getStepZ() * 0.49D;

        Direction horizontal =
                direction.getClockWise();

        double halfWidth =
                width / 2.0D;

        double halfHeight =
                height / 2.0D;

        double thickness = 0.0625D;

        double minX;
        double maxX;
        double minZ;
        double maxZ;

        if (horizontal.getAxis() == Direction.Axis.X) {

            minX = centerX - halfWidth;
            maxX = centerX + halfWidth;

            minZ = centerZ - thickness;
            maxZ = centerZ + thickness;

        } else {

            minX = centerX - thickness;
            maxX = centerX + thickness;

            minZ = centerZ - halfWidth;
            maxZ = centerZ + halfWidth;
        }

        double minY = centerY - halfHeight;
        double maxY = centerY + halfHeight;

        AABB result = new AABB(
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
        );

        /*
         * ========================================
         * DEBUG
         * ========================================
         */

//        System.out.println(
//                "[GhostPainting DEBUG] calculateBoundingBox"
//                        + " client=" + level().isClientSide()
//                        + " entity=" + getId()
//                        + " direction=" + direction
//                        + " horizontal=" + horizontal
//                        + " pos=" + pos
//                        + " variant=" + variant.width()
//                        + "x" + variant.height()
//                        + " AABB="
//                        + result
//                        + " size="
//                        + result.getXsize()
//                        + " x "
//                        + result.getYsize()
//                        + " x "
//                        + result.getZsize()
//        );

        return result;
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

//        System.out.println(
//                "[GhostPainting DEBUG] BEFORE tick"
//                        + " client=" + level().isClientSide()
//                        + " entity=" + getId()
//                        + " direction=" + getDirection()
//                        + " BB=" + getBoundingBox()
//                        + " size="
//                        + getBoundingBox().getXsize()
//                        + " x "
//                        + getBoundingBox().getYsize()
//                        + " x "
//                        + getBoundingBox().getZsize()
//        );

        super.tick();

//        System.out.println(
//                "[GhostPainting DEBUG] AFTER tick"
//                        + " client=" + level().isClientSide()
//                        + " entity=" + getId()
//                        + " direction=" + getDirection()
//                        + " BB=" + getBoundingBox()
//                        + " size="
//                        + getBoundingBox().getXsize()
//                        + " x "
//                        + getBoundingBox().getYsize()
//                        + " x "
//                        + getBoundingBox().getZsize()
//        );
    }

    @Override
    public void refreshDimensions() {

//        System.out.println(
//                "[GhostPainting DEBUG] refreshDimensions"
//                        + " client=" + level().isClientSide()
//                        + " entity=" + getId()
//                        + " direction=" + getDirection()
//                        + " BEFORE=" + getBoundingBox()
//        );

        super.refreshDimensions();

//        System.out.println(
//                "[GhostPainting DEBUG] refreshDimensions AFTER"
//                        + " client=" + level().isClientSide()
//                        + " entity=" + getId()
//                        + " direction=" + getDirection()
//                        + " AFTER=" + getBoundingBox()
//        );
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

        System.out.println(
                "[GhostPainting DEBUG] after facing"
                        + " entityPos=" + position()
                        + " blockPosition=" + blockPosition()
                        + " direction=" + getDirection()
                        + " BB=" + getBoundingBox()
        );
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(
            ServerEntity entity
    ) {
        return new ClientboundAddEntityPacket(
                this,
                this.getDirection().get3DDataValue(),
                this.getPos()
        );
    }

    @Override
    public void recreateFromPacket(
            ClientboundAddEntityPacket packet
    ) {
        super.recreateFromPacket(packet);

        setDirection(
                Direction.from3DDataValue(
                        packet.getData()
                )
        );

        recalculateBoundingBox();
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
     * 强制存活
     * ========================================
     */

    @Override
    public boolean survives() {
        return true;
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

//        System.out.println(
//                "[GhostPainting DEBUG] onSyncedDataUpdated"
//                        + " client=" + level().isClientSide()
//                        + " entity=" + getId()
//                        + " key=" + key
//                        + " direction=" + getDirection()
//        );

        recalculateBoundingBox();
    }
}