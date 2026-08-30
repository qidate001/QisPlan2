package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
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

    /**
     * 鬼在玩家身后的高度。
     */
    private static final double FOLLOW_HEIGHT = 1.0D;


    /*
     * ========================================
     * 喊名
     * ========================================
     */

    /**
     * 两次喊名之间的最小冷却。
     *
     * 这里暂时设置为 10 秒。
     */
    private static final int CALL_COOLDOWN_TICKS = 200;

    /**
     * 喊名次数上限。
     */
    private static final int MAX_CALL_COUNT = 10;

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
     * 玩家一次 Tick 内至少旋转这么多度，
     * 才认为玩家进行了回头。
     */
    private static final float TURN_THRESHOLD = 135.0F;

    /**
     * 上一次记录的玩家水平朝向。
     */
    private float lastPlayerYRot;

    /**
     * 是否已经拥有上一 Tick 的朝向。
     */
    private boolean hasLastPlayerRotation = false;


    /*
     * ========================================
     * NBT
     * ========================================
     */

    private static final String NBT_TARGET_PLAYER =
            "QisPlan2CallingGhostTarget";

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

        /*
         * 只由服务端控制。
         */
        if (level().isClientSide()) {
            return;
        }

        /*
         * 死机以后停止行为。
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
         * 没有目标时寻找新的玩家。
         */
        if (player == null) {

            clearTarget();

            player = findNearestPlayer();

            if (player != null) {

                setTargetPlayer(player);

                followPlayer(player);
            }
        }

        /*
         * 附近没有玩家。
         */
        if (player == null) {
            return;
        }

        /*
         * 玩家死亡。
         */
        if (!player.isAlive()) {

            clearTarget();

            return;
        }

        /*
         * ========================================
         * 玩家回头检测
         * ========================================
         */

        if (hasLastPlayerRotation) {

            float currentYRot =
                    player.getYRot();

            float rotationDelta =
                    Math.abs(
                            net.minecraft.util.Mth.wrapDegrees(
                                    currentYRot - lastPlayerYRot
                            )
                    );

            if (rotationDelta >= TURN_THRESHOLD) {

                onPlayerTurnAround(player);

                return;
            }
        }

        /*
         * 记录本 Tick 玩家朝向。
         */
        lastPlayerYRot =
                player.getYRot();

        hasLastPlayerRotation = true;


        /*
         * ========================================
         * 跟随玩家
         * ========================================
         */

        followPlayer(player);


        /*
         * ========================================
         * 喊名计时
         * ========================================
         */

        tickCalling(player);
    }


    /*
     * ========================================
     * 获取目标玩家
     * ========================================
     */

    private ServerPlayer getTargetPlayer() {

        if (!(level()
                instanceof ServerLevel serverLevel)) {

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

            /*
             * 如果玩家已经跨维度，
             * 当前实体不能继续跟随。
             */
            if (serverPlayer.level() != level()) {
                return null;
            }

            return serverPlayer;
        }

        return null;
    }


    private ServerPlayer findNearestPlayer() {

        if (!(level()
                instanceof ServerLevel serverLevel)) {

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

        callCount = 0;

        callCooldown =
                CALL_COOLDOWN_TICKS;

        /*
         * 绑定玩家时立即记录当前朝向。
         *
         * 防止刚找到玩家的第一 tick
         * 因为没有历史朝向而误判。
         */
        lastPlayerYRot =
                player.getYRot();

        hasLastPlayerRotation = true;
    }


    public UUID getTargetPlayerUUID() {

        return targetPlayerUUID;
    }


    /*
     * ========================================
     * 放弃当前目标
     * ========================================
     */

    private void clearTarget() {

        targetPlayerUUID = null;

        callCount = 0;

        callCooldown =
                CALL_COOLDOWN_TICKS;

        hasLastPlayerRotation = false;
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
         */
        Vec3 horizontalLook =
                new Vec3(
                        look.x,
                        0.0D,
                        look.z
                );

        /*
         * 防止 normalize 出问题。
         */
        if (horizontalLook.lengthSqr()
                < 1.0E-6D) {

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
                player.getY() + FOLLOW_HEIGHT,
                player.getZ() + behind.z
        );

        /*
         * 鬼和玩家保持相同朝向。
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
     * 玩家回头
     * ========================================
     */

    private void onPlayerTurnAround(
            ServerPlayer player
    ) {

        System.out.println(
                "[QisPlan2] 喊人鬼检测到 "
                        + player.getGameProfile().getName()
                        + " 回头！"
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
             * 已经喊满十次。
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
         * 是否已经喊满十次
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

        /*
         * 目前先测试。
         *
         * 后面这里正式接声音系统。
         */
        QisPlan2.LOGGER.info(
                "[QisPlan2] 喊人鬼喊：" +
                        player.getGameProfile().getName()
                        + "（第 "
                        + (callCount + 1)
                        + " 次）"
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