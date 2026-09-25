package com.qidate.qisplan2.entity.calling;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.entity.AbstractGhostEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
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
     * 当前距离下一次喊名还有多少 tick。
     */
    int callCooldown = 0;

    /**
     * 当前已经喊了多少次。
     */
    int callCount = 0;


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

        CallingGhostCallSystem.tick(
                this,
                player
        );
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

        CallingGhostMovementSystem.startFollowing(
                this,
                player
        );

        CallingGhostCallSystem.startCalling(
                this
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
     * NBT
     * ========================================
     */

    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(tag);

        CallingGhostTargetSystem.save(
                this,
                tag
        );

        CallingGhostCallSystem.save(
                this,
                tag
        );
    }


    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(tag);

        CallingGhostTargetSystem.load(
                this,
                tag
        );

        CallingGhostCallSystem.load(
                this,
                tag
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