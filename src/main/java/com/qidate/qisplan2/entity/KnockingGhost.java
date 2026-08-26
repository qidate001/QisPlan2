package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.entity.ai.GhostWanderGoal;
import com.qidate.qisplan2.ghost.ability.knockingghost.KnockingGhostDoorSystem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public class KnockingGhost
        extends AbstractGhostEntity {

    /*
     * ============================================================
     * 灵异防御
     * ============================================================
     */

    private static final double SUPERNATURAL_DEFENSE = 6.0D;

    @Override
    public double getSupernaturalDefense() {
        return SUPERNATURAL_DEFENSE;
    }

    /*
     * ============================================================
     * 寻找门
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
     * 两次敲门之间至少等待。
     */
    private static final int KNOCK_COOLDOWN = 10;

    /*
     * ============================================================
     * 构造
     * ============================================================
     */

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
     * AI
     * ============================================================
     */

    @Override
    protected void registerGoals()
    {

        /*
         * 找门 → 走门 → 敲门。
         *
         * 没有主动攻击目标。
         */
        this.goalSelector.addGoal(
                1,
                new KnockDoorGoal(
                        this,
                        1.0D
                )
        );

        /*
         * ========================================
         * 没有目标，四处游荡
         * ========================================
         */
        this.goalSelector.addGoal(
                8,
                new GhostWanderGoal(
                        this,
                        0.7D
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
         * 灵异死机
         * ========================================================
         *
         * AbstractGhostEntity 负责真正的死机状态。
         *
         * 这里仅仅阻止敲门鬼继续移动。
         */
        if (isSupernaturallyStunned()) {

            getNavigation().stop();

            return;
        }
    }

    /*
     * ============================================================
     * 寻找最近的门
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

                    /*
                     * ====================================================
                     * 最近敲过的门暂时跳过。
                     * ====================================================
                     */
                    if (KnockingGhostDoorSystem
                            .wasRecentlyKnocked(
                                    this,
                                    pos
                            )) {

                        continue;
                    }

                    /*
                     * ====================================================
                     * 鬼门 / 普通门优先级
                     * ====================================================
                     */

                    boolean ghostDoor =
                            KnockingGhostDoorSystem.isGhostDoor(
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

                    /*
                     * ====================================================
                     * 必须是真正的门下半部分。
                     * ====================================================
                     */

                    if (!KnockingGhostDoorSystem.isDoor(
                            level(),
                            pos
                    )) {

                        continue;
                    }

                    /*
                     * ====================================================
                     * 最近门。
                     * ====================================================
                     */

                    double distance =
                            center.distSqr(
                                    pos
                            );

                    if (distance
                            < bestDistance) {

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

        /*
         * ========================================================
         * 开始执行
         * ========================================================
         */

        @Override
        public boolean canUse() {

            /*
             * 死机时不寻找门。
             */
            if (ghost.isSupernaturallyStunned()) {
                return false;
            }

            /*
             * 敲门冷却。
             */
            if (cooldown > 0) {

                cooldown--;

                return false;
            }

            /*
             * 寻找目标门。
             */
            targetDoor =
                    ghost.findNearestDoor();

            return targetDoor != null;
        }

        /*
         * ========================================================
         * 是否继续
         * ========================================================
         */

        @Override
        public boolean canContinueToUse() {

            if (ghost.isSupernaturallyStunned()) {
                return false;
            }

            return targetDoor != null;
        }

        /*
         * ========================================================
         * 开始
         * ========================================================
         */

        @Override
        public void start() {

            if (targetDoor == null) {
                return;
            }

            moveToDoor();
        }

        /*
         * ========================================================
         * 停止
         * ========================================================
         */

        @Override
        public void stop() {

            ghost.getNavigation().stop();

            targetDoor = null;
        }

        /*
         * ========================================================
         * Tick
         * ========================================================
         */

        @Override
        public void tick() {

            if (targetDoor == null) {
                return;
            }

            /*
             * ====================================================
             * 必须在服务器。
             * ====================================================
             */

            if (!(ghost.level()
                    instanceof ServerLevel serverLevel)) {

                return;
            }

            /*
             * ====================================================
             * 门已经不存在。
             * ====================================================
             */

            if (!KnockingGhostDoorSystem.isDoor(
                    serverLevel,
                    targetDoor
            )) {

                targetDoor = null;

                return;
            }

            /*
             * ====================================================
             * 面向门。
             * ====================================================
             */

            ghost.getLookControl().setLookAt(
                    targetDoor.getX() + 0.5D,
                    targetDoor.getY() + 0.5D,
                    targetDoor.getZ() + 0.5D,
                    30.0F,
                    30.0F
            );

            /*
             * ====================================================
             * 计算距离。
             * ====================================================
             */

            double distanceSqr =
                    ghost.distanceToSqr(
                            targetDoor.getX() + 0.5D,
                            targetDoor.getY(),
                            targetDoor.getZ() + 0.5D
                    );

            /*
             * ====================================================
             * 距离 5 格以内：
             *
             * 不再寻路。
             * 直接瞬移到门前。
             * ====================================================
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
                     * 找不到合适位置时，
                     * 保持原来的兜底方案。
                     */
                    ghost.teleportTo(
                            targetDoor.getX() + 0.5D,
                            targetDoor.getY(),
                            targetDoor.getZ() + 0.5D
                    );
                }

                ghost.getNavigation().stop();

                /*
                 * =================================================
                 * 真正敲门。
                 *
                 * 所有规则：
                 *
                 * · 敲门声
                 * · 听声范围
                 * · 0.5 秒灵异攻击延迟
                 * · 鬼门
                 * · 50 格传播
                 * · 50 层递归
                 *
                 * 全部交给公共系统。
                 * =================================================
                 */

                KnockingGhostDoorSystem.knock(
                        serverLevel,
                        ghost,
                        targetDoor
                );

                /*
                 * 当前目标结束。
                 */
                targetDoor = null;

                /*
                 * 下一次寻找前短暂冷却。
                 */
                cooldown =
                        KNOCK_COOLDOWN;

                return;
            }

            /*
             * ====================================================
             * 还比较远：
             *
             * 正常寻路。
             * ====================================================
             */

            moveToDoor();
        }

        /*
         * ========================================================
         * 前往门
         * ========================================================
         */

        private void moveToDoor() {

            if (targetDoor == null) {
                return;
            }

            BlockPos bestApproach =
                    findBestApproachPosition();

            /*
             * ====================================================
             * 有合适门前位置：
             * 往门旁边走。
             * ====================================================
             */

            if (bestApproach != null) {

                ghost.getNavigation().moveTo(
                        bestApproach.getX() + 0.5D,
                        bestApproach.getY(),
                        bestApproach.getZ() + 0.5D,
                        speedModifier
                );

                return;
            }

            /*
             * ====================================================
             * 找不到门前位置：
             *
             * 直接朝门走。
             * ====================================================
             */

            ghost.getNavigation().moveTo(
                    targetDoor.getX() + 0.5D,
                    targetDoor.getY(),
                    targetDoor.getZ() + 0.5D,
                    speedModifier
            );
        }

        /*
         * ========================================================
         * 寻找最佳门前位置
         * ========================================================
         */

        private BlockPos findBestApproachPosition() {

            if (targetDoor == null) {
                return null;
            }

            BlockPos best =
                    null;

            double bestDistance =
                    Double.MAX_VALUE;

            /*
             * ====================================================
             * 四个水平面。
             * ====================================================
             */

            for (Direction direction :
                    Direction.Plane.HORIZONTAL) {

                BlockPos pos =
                        targetDoor.relative(
                                direction
                        );

                /*
                 * =================================================
                 * 人可以站进去。
                 * =================================================
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
                 * =================================================
                 * 脚下必须有支撑。
                 * =================================================
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

                /*
                 * =================================================
                 * 选择离自己最近的位置。
                 * =================================================
                 */

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
}