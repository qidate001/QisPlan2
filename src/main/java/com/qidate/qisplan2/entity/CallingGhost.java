package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class CallingGhost extends AbstractGhostEntity {

    /*
     * ========================================
     * 目标玩家
     * ========================================
     */

    private UUID targetPlayerUUID;

    /**
     * 最近一次被袭击的玩家。
     *
     * 短时间内不会再次选择这个玩家。
     */
    private UUID recentlyAttackedPlayerUUID;

    /**
     * 最近一次袭击目标的冷却时间。
     */
    private int attackTargetCooldown = 0;

    /**
     * 同一个玩家被袭击后的目标冷却。
     *
     * 这里暂时设置为 30 秒。
     */
    private static final int ATTACK_TARGET_COOLDOWN_TICKS = 20 * 30;


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
     */
    private static final int CALL_COOLDOWN_TICKS = 200;

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
     * 玩家一次 Tick 内至少旋转这么多度，
     * 才认为玩家进行了回头。
     */
    private static final float TURN_THRESHOLD = 30.0F;

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
         * ========================================
         * 最近袭击目标冷却
         * ========================================
         */

        if (attackTargetCooldown > 0) {

            attackTargetCooldown--;

            if (attackTargetCooldown <= 0) {

                recentlyAttackedPlayerUUID = null;
            }
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

        if (!canTrackPlayer(player)) {

            /*
             * 鬼石砖 / 关闭鬼门隔绝。
             *
             * 当前玩家暂时无法被喊人鬼追踪。
             */
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

    private boolean canTargetPlayer(
            ServerPlayer player
    ) {

        /*
         * 创造模式不追踪。
         */
        if (player.isCreative()) {
            return false;
        }

        /*
         * 旁观模式不追踪。
         */
        if (player.isSpectator()) {
            return false;
        }

        /*
         * 必须存活。
         */
        if (!player.isAlive()) {
            return false;
        }

        /*
         * 鬼石砖 / 关闭鬼门
         * 可以隔绝喊人鬼。
         */
        if (!canTrackPlayer(player)) {
            return false;
        }

        /*
         * 最近刚刚袭击过的玩家，
         * 暂时不能再次成为目标。
         */
        if (recentlyAttackedPlayerUUID != null
                && player.getUUID().equals(
                recentlyAttackedPlayerUUID
        )) {

            return false;
        }

        return true;
    }


    private ServerPlayer findNearestPlayer() {

        if (!(level()
                instanceof ServerLevel serverLevel)) {

            return null;
        }

        double maxDistanceSqr =
                64.0D * 64.0D;

        ServerPlayer nearestPlayer = null;

        double nearestDistanceSqr =
                Double.MAX_VALUE;

        for (ServerPlayer player
                : serverLevel.players()) {

            double distanceSqr =
                    distanceToSqr(player);

            if (distanceSqr > maxDistanceSqr) {
                continue;
            }

            if (!canTargetPlayer(player)) {
                continue;
            }

            if (recentlyAttackedPlayerUUID != null
                    && player.getUUID().equals(
                    recentlyAttackedPlayerUUID
            )) {

                continue;
            }

            if (distanceSqr < nearestDistanceSqr) {

                nearestDistanceSqr =
                        distanceSqr;

                nearestPlayer =
                        player;
            }
        }

        if (nearestPlayer != null) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 喊人鬼找到新目标：{}",
                    nearestPlayer.getGameProfile().getName()
            );
        }

        return nearestPlayer;
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

        /*
         * 没有目标以后立即停止移动。
         */
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
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

        QisPlan2.LOGGER.info(
                "[QisPlan2] 喊人鬼检测到 {} 回头！",
                player.getGameProfile().getName()
        );

        /*
         * ========================================
         * 记录最近袭击目标
         * ========================================
         */

        recentlyAttackedPlayerUUID =
                player.getUUID();

        attackTargetCooldown =
                ATTACK_TARGET_COOLDOWN_TICKS;

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
     * 灵异阻隔
     * ========================================
     */

    private boolean canTrackPlayer(
            ServerPlayer player
    ) {

        Vec3 start =
                position().add(
                        0.0D,
                        0.8D,
                        0.0D
                );

        Vec3 end =
                player.getEyePosition();

        double distance =
                start.distanceTo(end);

        int steps =
                Math.max(
                        1,
                        (int) Math.ceil(
                                distance / 0.5D
                        )
                );

        for (int i = 1; i < steps; i++) {

            double t =
                    (double) i / steps;

            double x =
                    Mth.lerp(
                            t,
                            start.x,
                            end.x
                    );

            double y =
                    Mth.lerp(
                            t,
                            start.y,
                            end.y
                    );

            double z =
                    Mth.lerp(
                            t,
                            start.z,
                            end.z
                    );

            BlockPos checkPos =
                    BlockPos.containing(
                            x,
                            y,
                            z
                    );

            BlockState state =
                    level().getBlockState(
                            checkPos
                    );

            if (isCallingGhostBlocker(state)) {
                return false;
            }
        }

        return true;
    }


    private boolean isCallingGhostBlocker(
            BlockState state
    ) {

        /*
         * 鬼石砖：
         *
         * 永久阻挡。
         */
        if (state.is(
                ModBlocks.GHOST_STONE_BRICKS.get()
        )) {

            return true;
        }

        /*
         * 鬼门：
         *
         * 关闭 → 阻挡
         * 打开 → 不阻挡
         */
        if (state.is(
                ModBlocks.GHOST_DOOR.get()
        )) {

            return !state.getValue(
                    DoorBlock.OPEN
            );
        }

        return false;
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

        QisPlan2.LOGGER.info(
                "[QisPlan2] CallingGhost.registerGoals()"
        );
    }
}