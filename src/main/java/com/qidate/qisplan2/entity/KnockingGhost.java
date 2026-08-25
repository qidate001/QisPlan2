package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.death.SupernaturalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class KnockingGhost
        extends PathfinderMob
        implements SupernaturalEntity {

    /*
     * ============================================================
     * 参数
     * ============================================================
     */

    /**
     * 寻找门的水平范围。
     */
    private static final int DOOR_SEARCH_RADIUS = 32;

    /**
     * 垂直搜索范围。
     */
    private static final int DOOR_SEARCH_VERTICAL = 6;

    /**
     * 玩家听到敲门声的半径。
     */
    private static final double KNOCK_HEARING_RADIUS = 16.0D;

    /**
     * 每次敲门造成的灵异攻击强度。
     */
    private static final double KNOCK_ATTACK_STRENGTH = 5.0D;

    /**
     * 两次敲门之间至少等待。
     */
    private static final int KNOCK_COOLDOWN = 10;

    /**
     * 同一扇门多久之后可以重新敲。
     */
    private static final int RECENT_DOOR_MEMORY_TIME = 20 * 30;

    /*
     * ============================================================
     * 死机状态
     * ============================================================
     */

    private int supernaturalStunTicks = 0;

    private boolean permanentSupernaturalStun = false;

    private static final String NBT_STUN_TICKS =
            "QisPlan2SupernaturalStunTicks";

    private static final String NBT_PERMANENT_STUN =
            "QisPlan2SupernaturalPermanentStun";

    /*
     * ============================================================
     * AI 状态
     * ============================================================
     */

    /**
     * 最近敲过的门。
     */
    private final Set<BlockPos> recentlyKnocked =
            new HashSet<>();

    /**
     * 最近敲门记录的计时。
     */
    private int recentDoorResetTicks = 0;

    public KnockingGhost(
            EntityType<? extends KnockingGhost> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );
    }

    /*
     * ============================================================
     * 灵异属性
     * ============================================================
     */

    /**
     * 这里按照你自己的设定调整。
     */
    private static final double SUPERNATURAL_DEFENSE = 6.0D;

    @Override
    public double getSupernaturalDefense() {
        return SUPERNATURAL_DEFENSE;
    }

    @Override
    public void onSupernaturalAttack(int ticks) {

        supernaturalStunTicks =
                (int) Math.min(
                        Integer.MAX_VALUE,
                        (long) supernaturalStunTicks
                                + ticks
                );

        getNavigation().stop();
    }

    @Override
    public void onPermanentSupernaturalAttack() {

        permanentSupernaturalStun = true;

        supernaturalStunTicks = 0;

        getNavigation().stop();
    }

    @Override
    public boolean isSupernaturallyStunned() {
        return permanentSupernaturalStun
                || supernaturalStunTicks > 0;
    }

    @Override
    public boolean isPermanentlySupernaturallyStunned() {
        return permanentSupernaturalStun;
    }

    /*
     * ============================================================
     * AI
     * ============================================================
     */

    @Override
    protected void registerGoals() {

        /*
         * 只有寻找并敲门。
         *
         * 没有 NearestAttackableTargetGoal。
         */
        this.goalSelector.addGoal(
                1,
                new KnockDoorGoal(
                        this,
                        1.0D
                )
        );
    }

    /*
     * ============================================================
     * Tick
     * ============================================================
     */

    @Override
    public void tick() {

        super.tick();

        /*
         * ========================================
         * 死机
         * ========================================
         */
        if (permanentSupernaturalStun) {

            getNavigation().stop();

            return;
        }

        if (supernaturalStunTicks > 0) {

            supernaturalStunTicks--;

            getNavigation().stop();

            return;
        }

        /*
         * ========================================
         * 最近门记录计时
         * ========================================
         */
        if (!level().isClientSide()) {

            if (recentDoorResetTicks > 0) {

                recentDoorResetTicks--;

            } else if (!recentlyKnocked.isEmpty()) {

                recentlyKnocked.clear();

                recentDoorResetTicks =
                        RECENT_DOOR_MEMORY_TIME;
            }
        }
    }

    /*
     * ============================================================
     * 寻找门
     * ============================================================
     */

    private BlockPos findNearestDoor() {

        BlockPos center =
                blockPosition();

        BlockPos bestDoor =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        int radius =
                DOOR_SEARCH_RADIUS;

        for (int x = -radius;
             x <= radius;
             x++) {

            for (int y = -DOOR_SEARCH_VERTICAL;
                 y <= DOOR_SEARCH_VERTICAL;
                 y++) {

                for (int z = -radius;
                     z <= radius;
                     z++) {

                    BlockPos pos =
                            center.offset(
                                    x,
                                    y,
                                    z
                            );

                    /*
                     * 已经敲过。
                     */
                    if (recentlyKnocked.contains(pos)) {
                        continue;
                    }

                    if (!isDoor(
                            level(),
                            pos
                    )) {
                        continue;
                    }

                    double distance =
                            center.distSqr(pos);

                    if (distance < bestDistance) {

                        bestDistance =
                                distance;

                        bestDoor =
                                pos.immutable();
                    }
                }
            }
        }

        return bestDoor;
    }

    /*
     * ============================================================
     * 门检测
     * ============================================================
     */

    private static boolean isDoor(
            Level level,
            BlockPos pos
    ) {

        BlockState state =
                level.getBlockState(pos);

        /*
         * 首选：
         * minecraft:doors
         *
         * 这样其他模组如果正确使用 Door Tag，
         * 也会自动兼容。
         */
        if (state.is(BlockTags.DOORS)) {
            return true;
        }

        /*
         * 兜底：
         * 原版 DoorBlock。
         */
        return state.getBlock()
                instanceof DoorBlock;
    }

    /*
     * ============================================================
     * 实际敲门
     * ============================================================
     */

    private void knockDoor(
            BlockPos doorPos
    ) {

        if (!(level()
                instanceof ServerLevel serverLevel)) {

            return;
        }

        /*
         * 门在抵达前可能已经消失。
         */
        if (!isDoor(
                serverLevel,
                doorPos
        )) {

            return;
        }

        /*
         * ========================================================
         * 播放敲门声
         * ========================================================
         *
         * null 表示广播给附近玩家。
         */
        serverLevel.playSound(
                null,
                doorPos,
                QisPlan2.GHOST_KNOCK.get(),
                net.minecraft.sounds.SoundSource.HOSTILE,
                1.0F,
                1.0F
        );

        /*
         * ========================================================
         * 听觉范围
         * ========================================================
         */
        AABB hearingBox =
                new AABB(
                        doorPos
                ).inflate(
                        KNOCK_HEARING_RADIUS
                );

        for (ServerPlayer player :
                serverLevel.getEntitiesOfClass(
                        ServerPlayer.class,
                        hearingBox
                )) {

            /*
             * 精确球形距离。
             */
            double dx =
                    player.getX()
                            - (doorPos.getX() + 0.5D);

            double dy =
                    player.getY()
                            - (doorPos.getY() + 0.5D);

            double dz =
                    player.getZ()
                            - (doorPos.getZ() + 0.5D);

            if (dx * dx
                    + dy * dy
                    + dz * dz
                    > KNOCK_HEARING_RADIUS
                    * KNOCK_HEARING_RADIUS) {

                continue;
            }

            /*
             * ====================================================
             * 听到敲门声
             * ====================================================
             */
            SupernaturalDeathHandler.tryKill(
                    player,
                    ModDamageTypes.knockingGhost(
                            this
                    ),
                    KNOCK_ATTACK_STRENGTH
            );
        }
    }

    /*
     * ============================================================
     * AI Goal
     * ============================================================
     */

    private static class KnockDoorGoal
            extends Goal {

        private final KnockingGhost ghost;

        private final double speedModifier;

        private BlockPos targetDoor;

        private int cooldown = 0;

        public KnockDoorGoal(
                KnockingGhost ghost,
                double speedModifier
        ) {
            this.ghost =
                    ghost;

            this.speedModifier =
                    speedModifier;

            setFlags(
                    EnumSet.of(
                            Flag.MOVE,
                            Flag.LOOK
                    )
            );
        }

        @Override
        public boolean canUse() {

            if (ghost.isSupernaturallyStunned()) {
                return false;
            }

            if (cooldown > 0) {

                cooldown--;

                return false;
            }

            targetDoor =
                    ghost.findNearestDoor();

            return targetDoor != null;
        }

        @Override
        public boolean canContinueToUse() {

            if (ghost.isSupernaturallyStunned()) {
                return false;
            }

            return targetDoor != null;
        }

        @Override
        public void start() {

            if (targetDoor == null) {
                return;
            }

            moveToDoor();
        }

        @Override
        public void stop() {

            ghost.getNavigation().stop();

            targetDoor = null;
        }

        @Override
        public void tick() {

            if (targetDoor == null) {
                return;
            }

            /*
             * 门没了。
             */
            if (!isDoor(
                    ghost.level(),
                    targetDoor
            )) {

                targetDoor = null;

                return;
            }

            /*
             * ========================================
             * 看向门
             * ========================================
             */
            ghost.getLookControl().setLookAt(
                    targetDoor.getX() + 0.5D,
                    targetDoor.getY() + 0.5D,
                    targetDoor.getZ() + 0.5D,
                    30.0F,
                    30.0F
            );

            /*
             * ========================================
             * 前往门
             * ========================================
             */
            double distanceSqr =
                    ghost.distanceToSqr(
                            targetDoor.getX() + 0.5D,
                            targetDoor.getY(),
                            targetDoor.getZ() + 0.5D
                    );

            /*
             * 进入门附近就敲。
             */
            if (distanceSqr > 3.0D) {

                moveToDoor();

                return;
            }

            /*
             * ========================================
             * 敲一次
             * ========================================
             */

            ghost.getNavigation().stop();

            ghost.knockDoor(
                    targetDoor
            );

            /*
             * ====================================================
             * 重点：
             *
             * 敲完以后，这扇门立刻进入 recentlyKnocked。
             *
             * 下一轮寻找时不会再选它。
             * ====================================================
             */
            ghost.recentlyKnocked.add(
                    targetDoor.immutable()
            );

            /*
             * 让最近门记忆持续 30 秒。
             */
            ghost.recentDoorResetTicks =
                    RECENT_DOOR_MEMORY_TIME;

            /*
             * 当前目标作废。
             */
            targetDoor = null;

            /*
             * 稍微休息一下，然后找下一扇。
             */
            cooldown =
                    KNOCK_COOLDOWN;
        }

        private void moveToDoor() {

            if (targetDoor == null) {
                return;
            }

            /*
             * 不直接走到门方块中心，
             * 而是尝试站在门旁边。
             *
             * 这样更像“站在门前敲门”。
             */
            BlockPos bestApproach =
                    findBestApproachPosition();

            if (bestApproach == null) {

                ghost.getNavigation().moveTo(
                        targetDoor.getX() + 0.5D,
                        targetDoor.getY(),
                        targetDoor.getZ() + 0.5D,
                        speedModifier
                );

                return;
            }

            ghost.getNavigation().moveTo(
                    bestApproach.getX() + 0.5D,
                    bestApproach.getY(),
                    bestApproach.getZ() + 0.5D,
                    speedModifier
            );
        }

        private BlockPos findBestApproachPosition() {

            BlockPos best =
                    null;

            double bestDistance =
                    Double.MAX_VALUE;

            for (Direction direction :
                    Direction.Plane.HORIZONTAL) {

                BlockPos pos =
                        targetDoor.relative(
                                direction
                        );

                if (!ghost.level()
                        .getBlockState(pos)
                        .getCollisionShape(
                                ghost.level(),
                                pos
                        )
                        .isEmpty()) {

                    continue;
                }

                double distance =
                        ghost.distanceToSqr(
                                pos.getX() + 0.5D,
                                pos.getY(),
                                pos.getZ() + 0.5D
                        );

                if (distance
                        < bestDistance) {

                    bestDistance =
                            distance;

                    best =
                            pos;
                }
            }

            return best;
        }
    }

    /*
     * ============================================================
     * 属性
     * ============================================================
     */

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
     * ============================================================
     * NBT
     * ============================================================
     */

    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(
                tag
        );

        tag.putInt(
                NBT_STUN_TICKS,
                supernaturalStunTicks
        );

        tag.putBoolean(
                NBT_PERMANENT_STUN,
                permanentSupernaturalStun
        );
    }

    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(
                tag
        );

        supernaturalStunTicks =
                Math.max(
                        0,
                        tag.getInt(
                                NBT_STUN_TICKS
                        )
                );

        permanentSupernaturalStun =
                tag.getBoolean(
                        NBT_PERMANENT_STUN
                );

        if (permanentSupernaturalStun) {
            supernaturalStunTicks = 0;
        }
    }
}