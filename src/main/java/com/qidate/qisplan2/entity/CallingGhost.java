package com.qidate.qisplan2.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class CallingGhost extends AbstractGhostEntity {

    /*
     * ========================================
     * 目标玩家
     * ========================================
     */

    private UUID targetPlayerUUID;


    /*
     * ========================================
     * 跟随参数
     * ========================================
     */

    /**
     * 鬼在玩家身后的距离。
     */
    private static final double FOLLOW_DISTANCE = 2.5D;


    /*
     * ========================================
     * 喊名
     * ========================================
     */

    /**
     * 暂时用于测试。
     *
     * 200 ticks = 10 秒。
     */
    private int callCooldown = 200;


    /*
     * ========================================
     * NBT
     * ========================================
     */

    private static final String NBT_TARGET_PLAYER =
            "QisPlan2CallingGhostTarget";

    private static final String NBT_CALL_COOLDOWN =
            "QisPlan2CallingGhostCallCooldown";


    /*
     * ========================================
     * 构造
     * ========================================
     */

    public CallingGhost(
            EntityType<? extends PathfinderMob> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );

        /*
         * 不使用实体物理。
         */
        this.noPhysics = true;

        /*
         * 不受重力。
         */
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {

        return Mob.createMobAttributes()
                .add(
                        Attributes.MAX_HEALTH,
                        20.0D
                )
                .add(
                        Attributes.MOVEMENT_SPEED,
                        0.25D
                )
                .add(
                        Attributes.FOLLOW_RANGE,
                        32.0D
                );
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
         * 只由服务端控制目标和位置。
         */
        if (level().isClientSide()) {
            return;
        }

        /*
         * 死机以后停止跟随。
         */
        if (isSupernaturallyStunned()) {
            return;
        }

        /*
         * 尝试获取已经绑定的玩家。
         */
        ServerPlayer player =
                getTargetPlayer();

        /*
         * 没有目标时，
         * 自动寻找最近玩家。
         */
        if (player == null) {

            player =
                    findNearestPlayer();

            if (player != null) {

                setTargetPlayer(
                        player
                );
            }
        }

        /*
         * 附近没有玩家。
         *
         * 暂时什么都不做。
         */
        if (player == null) {
            return;
        }

        /*
         * 玩家已经死亡。
         */
        if (!player.isAlive()) {
            return;
        }

        /*
         * 跟随玩家。
         */
        followPlayer(player);

        /*
         * 喊名计时。
         */
        tickCalling(player);
    }


    /*
     * ========================================
     * 获取目标玩家
     * ========================================
     */

    private ServerPlayer getTargetPlayer() {

        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (targetPlayerUUID == null) {
            return null;
        }

        Player player =
                serverLevel.getPlayerByUUID(
                        targetPlayerUUID
                );

        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }

        return null;
    }

    private ServerPlayer findNearestPlayer() {

        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Player player =
                serverLevel.getNearestPlayer(
                        this,
                        64.0D
                );

        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }

        return null;
    }


    /*
     * ========================================
     * 设置目标玩家
     * ========================================
     */

    public void setTargetPlayer(
            ServerPlayer player
    ) {
        targetPlayerUUID =
                player.getUUID();
    }


    public UUID getTargetPlayerUUID() {

        return targetPlayerUUID;
    }


    /*
     * ========================================
     * 跟随玩家
     * ========================================
     */

    private void followPlayer(
            ServerPlayer player
    ) {

        /*
         * 玩家的视线方向。
         */
        Vec3 look =
                player.getLookAngle();

        /*
         * 只取水平面。
         *
         * 玩家抬头、低头不会导致鬼飞到
         * 天上或地下。
         */
        Vec3 horizontalLook =
                new Vec3(
                        look.x,
                        0.0D,
                        look.z
                );

        /*
         * 防止极端情况下 normalize 出问题。
         */
        if (horizontalLook.lengthSqr() < 1.0E-6D) {
            return;
        }

        horizontalLook =
                horizontalLook.normalize();

        /*
         * 玩家视线反方向 = 玩家身后。
         */
        Vec3 behind =
                horizontalLook.scale(
                        -FOLLOW_DISTANCE
                );

        /*
         * 设置鬼的位置。
         */
        setPos(
                player.getX() + behind.x,
                player.getY(),
                player.getZ() + behind.z
        );

        /*
         * 鬼朝向和玩家一致。
         */
        setYRot(
                player.getYRot()
        );

        setXRot(
                0.0F
        );
    }


    /*
     * ========================================
     * 喊名
     * ========================================
     */

    private void tickCalling(
            ServerPlayer player
    ) {

        if (callCooldown > 0) {

            callCooldown--;

            return;
        }

        /*
         * 10 秒一次。
         */
        callCooldown = 200;

        /*
         * 下一阶段在这里真正播放名字。
         */
        callPlayerName(player);
    }


    private void callPlayerName(
            ServerPlayer player
    ) {

        /*
         * 现在先测试。
         */
        System.out.println(
                "[QisPlan2] 喊人鬼喊：" +
                        player.getGameProfile().getName()
        );
    }


    /*
     * ========================================
     * NBT
     * ========================================
     */

    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {

        super.addAdditionalSaveData(
                tag
        );

        if (targetPlayerUUID != null) {

            tag.putUUID(
                    NBT_TARGET_PLAYER,
                    targetPlayerUUID
            );
        }

        tag.putInt(
                NBT_CALL_COOLDOWN,
                callCooldown
        );
    }


    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {

        super.readAdditionalSaveData(
                tag
        );

        if (tag.hasUUID(
                NBT_TARGET_PLAYER
        )) {

            targetPlayerUUID =
                    tag.getUUID(
                            NBT_TARGET_PLAYER
                    );
        }

        callCooldown =
                Math.max(
                        0,
                        tag.getInt(
                                NBT_CALL_COOLDOWN
                        )
                );
    }


    /*
     * ========================================
     * AI
     * ========================================
     *
     * 喊人鬼不使用 GhostWanderGoal。
     */

    @Override
    protected void registerGoals() {
        // 故意不注册 GhostWanderGoal。
        //
        // 它的位置完全由 followPlayer()
        // 控制。
    }
}