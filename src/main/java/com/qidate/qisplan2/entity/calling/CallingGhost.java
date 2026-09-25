package com.qidate.qisplan2.entity.calling;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.entity.AbstractGhostEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class CallingGhost extends AbstractGhostEntity {

    /*
     * ========================================
     * 目标玩家
     * ========================================
     */

    UUID targetPlayerUUID;

    /**
     * 最近一次被袭击的玩家。
     *
     * 短时间内不会再次选择这个玩家。
     */
    UUID recentlyAttackedPlayerUUID;

    /**
     * 最近一次袭击目标的冷却时间。
     */
    int attackTargetCooldown = 0;


    /*
     * ========================================
     * 喊名
     * ========================================
     */

    /**
     * 两次喊名之间的最小冷却。
     */
    private static final int CALL_COOLDOWN_TICKS = 100;

    /**
     * 喊名次数上限。
     */
    private static final int MAX_CALL_COUNT = 7;

    /**
     * 当前距离下一次喊名还有多少 tick。
     */
    private int callCooldown = CALL_COOLDOWN_TICKS;

    /**
     * 当前已经喊了多少次。
     */
    private int callCount = 0;


    /*
     * ========================================
     * 回头检测
     * ========================================
     */

    /**
     * 上一次记录的玩家水平朝向。
     */
    float lastPlayerYRot;

    /**
     * 是否已经拥有上一 Tick 的朝向。
     */
    boolean hasLastPlayerRotation = false;


    /*
     * ========================================
     * NBT
     * ========================================
     */

    private static final String NBT_CALL_COOLDOWN =
            "QisPlan2CallingGhostCallCooldown";

    private static final String NBT_CALL_COUNT =
            "QisPlan2CallingGhostCallCount";
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
                        64.0D
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

        if (level().isClientSide()) {
            return;
        }

        if (isSupernaturallyStunned()) {
            return;
        }

        CallingGhostTargetSystem.tick(this);

        ServerPlayer player =
                CallingGhostTargetSystem.getTargetPlayer(this);

        if (player == null) {
            return;
        }

        if (!CallingGhostTargetSystem.canTrackPlayer(
                this,
                player
        )) {
            return;
        }

        if (CallingGhostMovementSystem.tick(
                this,
                player
        )) {
            onPlayerTurnAround(player);
            return;
        }

        tickCalling(player);
    }


    /*
     * ========================================
     * 设置目标玩家
     * ========================================
     */

    public void setTargetPlayer(
            ServerPlayer player
    ) {
        CallingGhostTargetSystem.setTargetPlayer(
                this,
                player
        );

        callCount = 0;
        callCooldown = CALL_COOLDOWN_TICKS;

        CallingGhostMovementSystem.startFollowing(
                this,
                player
        );
    }


    public UUID getTargetPlayerUUID() {

        return targetPlayerUUID;
    }


    /*
     * ========================================
     * 放弃当前目标
     * ========================================
     */

    public void clearTarget() {

        CallingGhostTargetSystem.clearTarget(
                this
        );

        callCount = 0;
        callCooldown = CALL_COOLDOWN_TICKS;

        CallingGhostMovementSystem.stopFollowing(
                this
        );

        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
    }


    /*
     * ========================================
     * 玩家回头
     * ========================================
     */

    private void onPlayerTurnAround(
            ServerPlayer player
    ) {

        QisPlan2.LOGGER.info(
                "[QisPlan2] 喊人鬼检测到 {} 回头！",
                player.getGameProfile().getName()
        );

        /*
         * ========================================
         * 记录最近袭击目标
         * ========================================
         */

        CallingGhostTargetSystem.markRecentlyAttacked(
                this,
                player
        );

        /*
         * ========================================
         * 30 强度灵异袭击
         * ========================================
         */

        SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.callingGhost(
                        this
                ),
                30.0D
        );

        /*
         * 回头以后放弃当前目标。
         */
        clearTarget();
    }


    /*
     * ========================================
     * 喊名
     * ========================================
     */

    private void tickCalling(
            ServerPlayer player
    ) {

        if (callCount >= MAX_CALL_COUNT) {

            /*
             * 已经喊满七次。
             *
             * 放弃当前玩家。
             */
            clearTarget();

            return;
        }


        if (callCooldown > 0) {

            callCooldown--;

            return;
        }


        /*
         * ========================================
         * 喊一次
         * ========================================
         */

        callPlayerName(
                player
        );

        callCount++;


        /*
         * ========================================
         * 是否已经喊满七次
         * ========================================
         */

        if (callCount >= MAX_CALL_COUNT) {

            clearTarget();

            return;
        }


        /*
         * 下一次喊名。
         */
        callCooldown =
                CALL_COOLDOWN_TICKS;
    }


    private void callPlayerName(
            ServerPlayer player
    ) {

        SoundEvent sound =
                CallingGhostSounds.getSound(
                        player
                );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 喊人鬼喊 {}：第 {} 次，音效={}",
                player.getGameProfile().getName(),
                callCount + 1,
                sound.getLocation()
        );

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.HOSTILE,
                1.0F,
                1.0F
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

        CallingGhostTargetSystem.save(
                this,
                tag
        );

        tag.putInt(
                NBT_CALL_COOLDOWN,
                callCooldown
        );

        tag.putInt(
                NBT_CALL_COUNT,
                callCount
        );
    }


    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(
                tag
        );

        CallingGhostTargetSystem.load(
                this,
                tag
        );

        callCooldown =
                Math.max(
                        0,
                        tag.getInt(
                                NBT_CALL_COOLDOWN
                        )
                );

        callCount =
                Math.max(
                        0,
                        tag.getInt(
                                NBT_CALL_COUNT
                        )
                );
    }


    /*
     * ========================================
     * AI
     * ========================================
     */

    @Override
    protected void registerGoals() {
        /*
         * 喊人鬼不使用任何 AI。
         *
         * 它的位置完全由 followPlayer()
         * 控制。
         */
    }
}