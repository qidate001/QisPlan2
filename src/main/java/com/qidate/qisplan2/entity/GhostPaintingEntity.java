package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.QisPlan2;
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
import net.minecraft.world.phys.Vec3;

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

        /*
         * 原版 Painting：
         *
         * Vec3.atCenterOf(pos)
         *      .relative(direction, -0.46875D)
         */
        Vec3 center = Vec3.atCenterOf(pos)
                .relative(direction, -0.46875D);

        /*
         * 偶数尺寸需要向对应方向偏移 0.5 格。
         */
        double widthOffset =
                offsetForPaintingSize(variant.width());

        double heightOffset =
                offsetForPaintingSize(variant.height());

        Direction horizontal =
                direction.getCounterClockWise();

        center = center
                .relative(horizontal, widthOffset)
                .relative(Direction.UP, heightOffset);

        /*
         * 原版 Painting 的碰撞箱尺寸。
         */
        Direction.Axis axis =
                direction.getAxis();

        double sizeX =
                axis == Direction.Axis.X
                        ? 0.0625D
                        : variant.width();

        double sizeY =
                variant.height();

        double sizeZ =
                axis == Direction.Axis.Z
                        ? 0.0625D
                        : variant.width();

        return AABB.ofSize(
                center,
                sizeX,
                sizeY,
                sizeZ
        );
    }

    private static double offsetForPaintingSize(int size) {
        return size % 2 == 0
                ? 0.5D
                : 0.0D;
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

    private BlockPos lastPos = BlockPos.ZERO;

    @Override
    public void tick() {

        BlockPos beforePos = this.pos;
        double beforeX = getX();
        double beforeY = getY();
        double beforeZ = getZ();

        super.tick();

        if (!beforePos.equals(this.pos)) {
            QisPlan2.LOGGER.warn(
                    """
                    [GhostPainting POS CHANGED]
                    entity={}
                    fieldPos: {} -> {}
                    worldPos: ({}, {}, {}) -> ({}, {}, {})
                    """,
                    getId(),
                    beforePos,
                    this.pos,
                    beforeX,
                    beforeY,
                    beforeZ,
                    getX(),
                    getY(),
                    getZ()
            );
        }
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
    }

    @Override
    public void moveTo(
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        setPos(x, y, z);
    }

    @Override
    public void lerpTo(
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            int steps
    ) {
        setPos(x, y, z);
    }

    @Override
    public Vec3 trackingPosition() {
        return Vec3.atLowerCornerOf(this.pos);
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

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
        super.readAdditionalSaveData(tag);

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

        if (key.equals(PAINTING)) {
            recalculateBoundingBox();
        }
    }

    @Override
    protected void checkInsideBlocks() {
        QisPlan2.LOGGER.info(
                "[GhostPainting] checkInsideBlocks client={}",
                level().isClientSide()
        );
    }
}