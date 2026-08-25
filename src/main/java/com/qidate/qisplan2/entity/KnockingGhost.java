package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalCombatHandler;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.death.SupernaturalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

import java.util.*;

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

    /**
     * 攻击奏效延迟
     */
    private static final int KNOCK_ATTACK_DELAY = 10;

    private final List<PendingKnockAttack>
            pendingKnockAttacks =
            new ArrayList<>();

    private record PendingKnockAttack(
            UUID target,
            int remainingTicks
    ) {
    }

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

    /**
     * 无敌。
     */
    @Override
    public boolean isInvulnerableTo(
            DamageSource damageSource
    ) {
        return SupernaturalCombatHandler.isInvulnerableTo(
                this,
                damageSource
        );
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

        if (!level().isClientSide()) {
            tickPendingKnockAttacks();
        }
    }

    private void tickPendingKnockAttacks() {

        if (pendingKnockAttacks.isEmpty()) {
            return;
        }

        for (int i =
             pendingKnockAttacks.size() - 1;
             i >= 0;
             i--) {

            PendingKnockAttack pending =
                    pendingKnockAttacks.get(i);

            int remaining =
                    pending.remainingTicks() - 1;

            if (remaining > 0) {

                pendingKnockAttacks.set(
                        i,
                        new PendingKnockAttack(
                                pending.target(),
                                remaining
                        )
                );

                continue;
            }

            /*
             * 时间到了。
             */
            if (level() instanceof ServerLevel serverLevel) {

                Entity entity =
                        serverLevel.getEntity(
                                pending.target()
                        );

                if (entity instanceof LivingEntity living
                        && living.isAlive()
                        && living != this) {

                    SupernaturalDeathHandler.tryKill(
                            living,
                            ModDamageTypes.knockingGhost(
                                    this
                            ),
                            KNOCK_ATTACK_STRENGTH
                    );
                }
            }

            pendingKnockAttacks.remove(i);
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
         * ========================================================
         * 原版 / 标准 DoorBlock
         * ========================================================
         *
         * 门只认下半部分。
         */
        if (state.getBlock() instanceof DoorBlock) {

            return state.getValue(
                    DoorBlock.HALF
            ) == DoubleBlockHalf.LOWER;
        }

        /*
         * ========================================================
         * 其他模组的门
         * ========================================================
         *
         * 如果它不是 DoorBlock，但加入了 DOORS 标签，
         * 先检查有没有 DOUBLE_BLOCK_HALF 属性。
         *
         * 有的话，只接受 LOWER。
         */
        if (state.is(BlockTags.DOORS)) {

            if (state.hasProperty(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF
            )) {

                return state.getValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF
                ) == DoubleBlockHalf.LOWER;
            }

            /*
             * 没有上下半属性：
             * 视为单方块门，直接接受。
             */
            return true;
        }

        return false;
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

        if (!isDoor(
                serverLevel,
                doorPos
        )) {
            return;
        }

        /*
         * ========================================================
         * 先播放敲门声
         * ========================================================
         */
        serverLevel.playSound(
                null,
                doorPos,
                QisPlan2.GHOST_KNOCK.get(),
                SoundSource.HOSTILE,
                1.0F,
                1.0F
        );

        /*
         * ========================================================
         * 查找听到敲门声的生物
         * ========================================================
         */
        AABB hearingBox =
                new AABB(doorPos)
                        .inflate(
                                KNOCK_HEARING_RADIUS
                        );

        for (LivingEntity entity :
                serverLevel.getEntitiesOfClass(
                        LivingEntity.class,
                        hearingBox,
                        LivingEntity::isAlive
                )) {

            /*
             * 敲门鬼自己不受到自己的敲门声攻击。
             */
            if (entity == this) {
                continue;
            }

            double dx =
                    entity.getX()
                            - (doorPos.getX() + 0.5D);

            double dy =
                    entity.getY()
                            - (doorPos.getY() + 0.5D);

            double dz =
                    entity.getZ()
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
             * 不立即攻击。
             *
             * 记录下来，10 tick 后再攻击。
             * ====================================================
             */
            pendingKnockAttacks.add(
                    new PendingKnockAttack(
                            entity.getUUID(),
                            KNOCK_ATTACK_DELAY
                    )
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
             * ========================================================
             * 距离门 5 格以内
             * ========================================================
             *
             * 不再继续寻路。
             *
             * 直接出现在门前。
             */
            if (distanceSqr <= 25.0D) {

                BlockPos approach =
                        findBestApproachPosition();

                if (approach != null) {

                    ghost.teleportTo(
                            approach.getX() + 0.5D,
                            approach.getY(),
                            approach.getZ() + 0.5D
                    );

                } else {

                    /*
                     * 如果周围没有合适的门前位置，
                     * 就直接到门的一侧。
                     */
                    ghost.teleportTo(
                            targetDoor.getX() + 0.5D,
                            targetDoor.getY(),
                            targetDoor.getZ() + 0.5D
                    );
                }

                ghost.getNavigation().stop();

                /*
                 * 瞬移完成以后立即敲门。
                 */
                ghost.knockDoor(
                        targetDoor
                );

                /*
                 * 记录这扇门。
                 */
                ghost.recentlyKnocked.add(
                        targetDoor.immutable()
                );

                ghost.recentDoorResetTicks =
                        RECENT_DOOR_MEMORY_TIME;

                /*
                 * 当前目标结束。
                 */
                targetDoor = null;

                /*
                 * 下一扇门。
                 */
                cooldown =
                        KNOCK_COOLDOWN;

                return;
            }


            /*
             * ========================================================
             * 还比较远
             * ========================================================
             */
            moveToDoor();

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

                /*
                 * 门前必须能站人。
                 */
                if (!ghost.level()
                        .getBlockState(pos)
                        .getCollisionShape(
                                ghost.level(),
                                pos
                        )
                        .isEmpty()) {

                    continue;
                }

                /*
                 * 脚下需要有支撑。
                 */
                BlockPos below =
                        pos.below();

                if (ghost.level()
                        .getBlockState(below)
                        .getCollisionShape(
                                ghost.level(),
                                below
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

                if (distance < bestDistance) {

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