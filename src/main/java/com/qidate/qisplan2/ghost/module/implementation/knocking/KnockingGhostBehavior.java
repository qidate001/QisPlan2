package com.qidate.qisplan2.ghost.module.implementation.knocking;
import com.qidate.qisplan2.ghost.module.host.entity.KnockingGhostEntityBinding;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.entity.ai.GhostWanderGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.List;

/**
 * 敲门鬼模块的实体行为。每个宿主创建独立的目标门与冷却状态，
 * 不把可变寻路状态保存在全局共享的模块定义中。
 */
public final class KnockingGhostBehavior {
    private static final int DOOR_SEARCH_RADIUS = 32;
    private static final int DOOR_SEARCH_VERTICAL = 6;
    private static final int KNOCK_COOLDOWN = 10;

    /**
     * 为敲门鬼创建独立的找门行为与游荡行为，避免跨实体共享临时目标。
     */
    public static List<Goal> create(ModularGhostEntity ghost) {
        return List.of(new KnockDoorGoal(ghost, 1.0D), new GhostWanderGoal(ghost, 0.7D) {
            /**
             * 判断当前行为是否可以开始执行。
             * 检查行为需要的模块状态、环境或目标条件，交由 Goal 调度器决定是否取得控制。
             */
            @Override public boolean canUse() {
                return KnockingGhostEntityBinding.isActive(ghost) && super.canUse();
            }

            /**
             * 重新检查行为是否仍可继续。
             * 条件不满足时由调度器停止行为，释放占用的控制资源。
             */
            @Override public boolean canContinueToUse() {
                return KnockingGhostEntityBinding.isActive(ghost) && super.canContinueToUse();
            }
        });
    }

    /**
     * 在搜索范围内寻找最近的可用门，并跳过最近敲过的位置。
     * 无筛选参数时优先鬼门；带筛选参数时只搜索对应类别。
     */
    private static BlockPos findNearestDoor(ModularGhostEntity ghost) {

        /*
         * ========================================================
         * 第一优先级：鬼门
         * ========================================================
         */

        BlockPos ghostDoor =
                findNearestDoor(
                        ghost, true
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
                ghost, false
        );
    }

    /**
     * 在搜索范围内寻找最近的可用门，并跳过最近敲过的位置。
     * 无筛选参数时优先鬼门；带筛选参数时只搜索对应类别。
     */
    private static BlockPos findNearestDoor(
            ModularGhostEntity ghost, boolean onlyGhostDoor
    ) {

        BlockPos center =
                ghost.blockPosition();

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
                                    ghost,
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
                                    ghost.level(),
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
                            ghost.level(),
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

        private final ModularGhostEntity ghost;

        private final double speedModifier;

        private BlockPos targetDoor;

        private int cooldown = 0;

        /**
         * 为指定实体建立独立的找门行为，并保存寻路速度倍率。
         * 声明移动与视线控制，目标门和短暂行为冷却保存在本 Goal 实例中。
         */
        public KnockDoorGoal(
                ModularGhostEntity ghost,
                double speedModifier
        ) {

            this.ghost = ghost;
            this.speedModifier = speedModifier;

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
            if (!KnockingGhostEntityBinding.isActive(ghost)) {
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
                    findNearestDoor(ghost);

            return targetDoor != null;
        }

        /*
         * ========================================================
         * 是否继续
         * ========================================================
         */

        @Override
        public boolean canContinueToUse() {

            if (!KnockingGhostEntityBinding.isActive(ghost)) {
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

                KnockingGhostEntityBinding.knock(ghost, targetDoor);

                /*
                 * 当前目标结束。
                 */
                targetDoor = null;

                /*
                 * 下一次寻找前短暂冷却。
                 */
                cooldown = KNOCK_COOLDOWN;

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

}
