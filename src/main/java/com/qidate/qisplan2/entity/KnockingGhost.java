package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.GhostDoorBlock;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
        extends AbstractGhostEntity {

    /*
     * ============================================================
     * 参数
     * ============================================================
     */

    /**
     * 灵异防御
     */
    private static final double SUPERNATURAL_DEFENSE = 6.0D;

    @Override
    public double getSupernaturalDefense() {
        return SUPERNATURAL_DEFENSE;
    }

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

    /**
     * 鬼门共鸣范围。
     */
    private static final int GHOST_DOOR_ECHO_RADIUS = 50;

    /**
     * 鬼门共鸣的垂直搜索范围。
     */
    private static final int GHOST_DOOR_ECHO_VERTICAL_RADIUS = 20;

    /**
     * 共鸣门最早什么时候响。
     */
    private static final int GHOST_DOOR_ECHO_MIN_DELAY = 5;

    /**
     * 共鸣门最晚什么时候响。
     */
    private static final int GHOST_DOOR_ECHO_MAX_DELAY = 40;

    /**
     * 鬼门共鸣最大递归深度。
     */
    private static final int MAX_GHOST_DOOR_ECHO_DEPTH = 50;

    private final List<PendingKnockAttack>
            pendingKnockAttacks =
            new ArrayList<>();

    private record PendingKnockAttack(
            UUID target,
            int remainingTicks
    ) { }

    /**
     * 延迟播放的敲门声。
     */
    private final List<PendingDoorKnock>
            pendingDoorKnocks =
            new ArrayList<>();

    private record PendingDoorKnock(
            BlockPos doorPos,

            /**
             * 距离最初敲门鬼敲下的鬼门，
             * 当前已经传播了多少层。
             */
            int echoDepth,

            int remainingTicks,

            /**
             * 同一轮共鸣共享的访问记录。
             */
            Set<BlockPos> visitedDoors
    ) { }

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
         * ========================================================
         * 死机时不允许敲门鬼继续寻路。
         *
         * AbstractGhostEntity 已经统一管理死机状态，
         * 这里只负责停止自己的 AI 移动。
         * ========================================================
         */
        if (isSupernaturallyStunned()) {

            getNavigation().stop();

            return;
        }

        /*
         * ========================================================
         * 最近门记录计时
         * ========================================================
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

        /*
         * ========================================================
         * 延迟灵异攻击
         * ========================================================
         */
        if (!level().isClientSide()) {
            tickPendingKnockAttacks();
        }

        /*
         * ========================================================
         * 延迟鬼门共鸣
         * ========================================================
         */
        if (!level().isClientSide()) {
            tickPendingDoorKnocks();
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

    private void tickPendingDoorKnocks() {

        if (pendingDoorKnocks.isEmpty()) {
            return;
        }

        for (int i =
             pendingDoorKnocks.size() - 1;
             i >= 0;
             i--) {

            PendingDoorKnock pending =
                    pendingDoorKnocks.get(i);

            int remaining =
                    pending.remainingTicks() - 1;

            if (remaining > 0) {

                pendingDoorKnocks.set(
                        i,
                        new PendingDoorKnock(
                                pending.doorPos(),
                                pending.echoDepth(),
                                remaining,
                                pending.visitedDoors()
                        )
                );

                continue;
            }

            BlockPos doorPos =
                    pending.doorPos();

            /*
             * ====================================================
             * 门仍然存在
             * ====================================================
             */
            if (level() instanceof ServerLevel serverLevel
                    && isDoor(
                    serverLevel,
                    doorPos
            )) {

                /*
                 * 正式让这扇门响。
                 *
                 * 它自己的 echoDepth 会继续向下一层传播。
                 */
                triggerDoorKnock(
                        serverLevel,
                        doorPos,
                        pending.echoDepth(),
                        pending.visitedDoors()
                );
            }

            pendingDoorKnocks.remove(i);
        }
    }

    /*
     * ============================================================
     * 寻找门
     * ============================================================
     */

    private BlockPos findNearestDoor() {

        /*
         * ========================================================
         * 第一优先级：鬼门
         * ========================================================
         */

        BlockPos ghostDoor =
                findNearestDoor(
                        true
                );

        if (ghostDoor != null) {
            return ghostDoor;
        }

        /*
         * ========================================================
         * 第二优先级：普通门
         * ========================================================
         */

        return findNearestDoor(
                false
        );
    }

    private BlockPos findNearestDoor(
            boolean onlyGhostDoor
    ) {

        BlockPos center =
                blockPosition();

        BlockPos best =
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

                    if (recentlyKnocked.contains(pos)) {
                        continue;
                    }

                    boolean ghostDoor =
                            isGhostDoor(
                                    level(),
                                    pos
                            );

                    if (onlyGhostDoor
                            && !ghostDoor) {
                        continue;
                    }

                    if (!onlyGhostDoor
                            && ghostDoor) {
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

                        best =
                                pos.immutable();
                    }
                }
            }
        }

        return best;
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

    private static boolean isGhostDoor(
            Level level,
            BlockPos pos
    ) {
        BlockState state =
                level.getBlockState(pos);

        /*
         * 如果你的 GhostDoorBlock 继承 DoorBlock，
         * 同样只接受下半部分。
         */
        if (state.getBlock()
                instanceof GhostDoorBlock) {

            if (state.hasProperty(
                    DoorBlock.HALF
            )) {

                return state.getValue(
                        DoorBlock.HALF
                ) == DoubleBlockHalf.LOWER;
            }

            return true;
        }

        return false;
    }

    /*
     * ============================================================
     * 实际敲门
     * ============================================================
     */

    private void triggerDoorKnock(
            ServerLevel serverLevel,
            BlockPos doorPos,
            int echoDepth,
            Set<BlockPos> visitedDoors
    ) {

        doorPos =
                doorPos.immutable();

        /*
         * 门已经不存在。
         */
        if (!isDoor(
                serverLevel,
                doorPos
        )) {
            return;
        }

        /*
         * ========================================================
         * 同一轮共鸣中：
         * 同一扇门只允许触发一次。
         * ========================================================
         */
        if (!visitedDoors.add(
                doorPos
        )) {
            return;
        }

        /*
         * ========================================================
         * 标记最近敲过
         * ========================================================
         */
        recentlyKnocked.add(
                doorPos
        );

        recentDoorResetTicks =
                RECENT_DOOR_MEMORY_TIME;

        /*
         * ========================================================
         * 播放敲门声
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
         * 安排灵异攻击
         * ========================================================
         */
        queueKnockAttacks(
                serverLevel,
                doorPos
        );

        /*
         * ========================================================
         * 鬼门递归
         * ========================================================
         */

        /*
         * 普通门：
         * 到此结束。
         */
        if (!isGhostDoor(
                serverLevel,
                doorPos
        )) {
            return;
        }

        /*
         * 已经达到最大层数。
         */
        if (echoDepth
                >= MAX_GHOST_DOOR_ECHO_DEPTH) {

            return;
        }

        /*
         * 继续向下一层传播。
         */
        scheduleGhostDoorEchoes(
                serverLevel,
                doorPos,
                echoDepth + 1,
                visitedDoors
        );
    }

    /*
     * ============================================================
     * 听到门声的生物
     * ============================================================
     */

    private void queueKnockAttacks(
            ServerLevel serverLevel,
            BlockPos doorPos
    ) {

        AABB hearingBox =
                new AABB(
                        doorPos
                ).inflate(
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
             * 10 tick 后攻击。
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
     * 鬼门的 50 格共鸣
     * ============================================================
     */

    private void scheduleGhostDoorEchoes(
            ServerLevel serverLevel,
            BlockPos sourceDoor,
            int echoDepth,
            Set<BlockPos> visitedDoors
    ) {

        int radius =
                GHOST_DOOR_ECHO_RADIUS;

        int vertical =
                GHOST_DOOR_ECHO_VERTICAL_RADIUS;

        Set<BlockPos> nearbyDoors =
                new HashSet<>();

        BlockPos.MutableBlockPos mutable =
                new BlockPos.MutableBlockPos();

        /*
         * ========================================================
         * 扫描 50 格鬼门领域
         * ========================================================
         */
        for (int x = -radius;
             x <= radius;
             x++) {

            for (int y = -vertical;
                 y <= vertical;
                 y++) {

                for (int z = -radius;
                     z <= radius;
                     z++) {

                    /*
                     * 圆形范围。
                     */
                    if (x * x + z * z
                            > radius * radius) {

                        continue;
                    }

                    mutable.set(
                            sourceDoor.getX() + x,
                            sourceDoor.getY() + y,
                            sourceDoor.getZ() + z
                    );

                    /*
                     * 不强制加载新区块。
                     */
                    if (!serverLevel.hasChunkAt(
                            mutable
                    )) {
                        continue;
                    }

                    BlockPos candidate =
                            mutable.immutable();

                    /*
                     * 自己跳过。
                     */
                    if (candidate.equals(
                            sourceDoor
                    )) {
                        continue;
                    }

                    /*
                     * 这轮共鸣已经访问过。
                     */
                    if (visitedDoors.contains(
                            candidate
                    )) {
                        continue;
                    }

                    /*
                     * 必须是门下半部分。
                     */
                    if (!isDoor(
                            serverLevel,
                            candidate
                    )) {
                        continue;
                    }

                    nearbyDoors.add(
                            candidate
                    );
                }
            }
        }

        /*
         * ========================================================
         * 安排这些门依次响起
         * ========================================================
         */

        for (BlockPos door :
                nearbyDoors) {

            /*
             * 由 triggerDoorKnock() 统一标记。
             */

            double dx =
                    door.getX()
                            - sourceDoor.getX();

            double dz =
                    door.getZ()
                            - sourceDoor.getZ();

            double distance =
                    Math.sqrt(
                            dx * dx
                                    + dz * dz
                    );

            int delay =
                    GHOST_DOOR_ECHO_MIN_DELAY
                            + (int) Math.min(
                            GHOST_DOOR_ECHO_MAX_DELAY
                                    - GHOST_DOOR_ECHO_MIN_DELAY,
                            distance / 8.0D
                    );

            pendingDoorKnocks.add(
                    new PendingDoorKnock(
                            door,
                            echoDepth,
                            delay,
                            visitedDoors
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
             * ========================================================
             * 必须是服务器世界
             * ========================================================
             */
            if (!(ghost.level()
                    instanceof ServerLevel serverLevel)) {

                return;
            }

            /*
             * ========================================================
             * 门没了
             * ========================================================
             */
            if (!isDoor(
                    serverLevel,
                    targetDoor
            )) {

                targetDoor = null;

                return;
            }

            /*
             * ========================================================
             * 看向门
             * ========================================================
             */
            ghost.getLookControl().setLookAt(
                    targetDoor.getX() + 0.5D,
                    targetDoor.getY() + 0.5D,
                    targetDoor.getZ() + 0.5D,
                    30.0F,
                    30.0F
            );

            /*
             * ========================================================
             * 计算距离
             * ========================================================
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
             * 直接瞬移到门前，
             * 然后敲门。
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
                     * 如果找不到合适的门前位置，
                     * 就直接到门中心附近。
                     */
                    ghost.teleportTo(
                            targetDoor.getX() + 0.5D,
                            targetDoor.getY(),
                            targetDoor.getZ() + 0.5D
                    );
                }

                ghost.getNavigation().stop();

                /*
                 * ====================================================
                 * 开始一轮新的鬼门共鸣
                 * ====================================================
                 */
                Set<BlockPos> visitedDoors =
                        new HashSet<>();

                ghost.triggerDoorKnock(
                        serverLevel,
                        targetDoor,
                        0,
                        visitedDoors
                );

                /*
                 * ====================================================
                 * 当前目标结束
                 * ====================================================
                 */
                targetDoor = null;

                /*
                 * 下一扇门之前稍微停顿。
                 */
                cooldown =
                        KNOCK_COOLDOWN;

                return;
            }

            /*
             * ========================================================
             * 距离还大于 5 格
             * ========================================================
             *
             * 正常寻路。
             */
            moveToDoor();
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
}