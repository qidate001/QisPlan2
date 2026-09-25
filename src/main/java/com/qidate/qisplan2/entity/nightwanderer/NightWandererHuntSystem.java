package com.qidate.qisplan2.entity.nightwanderer;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 夜游鬼猎杀系统。
 *
 * <p>
 * 负责夜游鬼在满足一定条件后，
 * 主动寻找目标并进行猎杀的特殊机制。
 *
 * <ul>
 *     <li>判断猎杀能力是否已经解锁</li>
 *     <li>统计夜游鬼连续无目标的时间</li>
 *     <li>寻找合适的猎杀目标</li>
 *     <li>判断目标是否处于黑暗环境</li>
 *     <li>寻找目标附近的黑暗位置</li>
 *     <li>验证瞬移位置是否安全</li>
 *     <li>执行猎杀瞬移并锁定目标</li>
 * </ul>
 */
public final class NightWandererHuntSystem {

    /**
     * 超过 100 次击杀后解锁猎杀能力。
     *
     * <p>
     * 100 次击杀：尚未解锁。
     * 101 次击杀：正式解锁。
     */
    private static final int HUNT_UNLOCK_KILLS = 100;

    /**
     * 连续没有锁定目标多久后，
     * 强制发动猎杀。
     *
     * <p>
     * 30 秒 = 600 tick。
     */
    private static final int HUNT_NO_TARGET_TIME = 30 * 20;

    /**
     * 猎杀目标搜索范围。
     *
     * <p>
     * 夜游鬼会在自身周围 64 格范围内
     * 搜索可以成为猎杀目标的 LivingEntity。
     */
    private static final double HUNT_TARGET_SEARCH_RANGE = 64.0D;

    /**
     * 猎杀传送后的冷却时间。
     *
     * <p>
     * 10 秒 = 200 tick。
     */
    private static final int HUNT_TELEPORT_COOLDOWN = 10 * 20;

    /**
     * 目标附近寻找传送位置的最大范围。
     */
    private static final double HUNT_TELEPORT_RADIUS = 12.0D;

    /**
     * 与目标保持的最小距离。
     *
     * <p>
     * 防止夜游鬼直接传送到目标实体内部。
     */
    private static final double HUNT_MIN_DISTANCE = 3.0D;

    /**
     * 猎杀机制使用的黑暗判定阈值。
     *
     * <p>
     * 方块光和天空光都不超过 7，
     * 才会被认为属于黑暗环境。
     */
    private static final int HUNT_DARKNESS_THRESHOLD = 7;

    private NightWandererHuntSystem() {
    }

    /**
     * 每 tick 更新夜游鬼的猎杀机制。
     *
     * <p>
     * 该方法只负责“猎杀能力”本身，
     * 不负责普通追踪和普通灵异攻击。
     *
     * @param ghost 当前夜游鬼
     */
    public static void tick(
            NightWanderer ghost
    ) {

        /*
         * ========================================
         * 是否已经解锁猎杀能力
         * ========================================
         *
         * 猎杀能力需要超过 100 次击杀
         * 才会正式解锁。
         */
        if (!hasHuntAbility(ghost)) {

            /*
             * 尚未解锁时，
             * 不应该保留之前可能存在的无目标计时。
             */
            ghost.resetHuntNoTargetTicks();

            return;
        }

        /*
         * ========================================
         * 瞬移冷却
         * ========================================
         *
         * 猎杀瞬移完成后的一段时间内，
         * 不允许再次进行猎杀瞬移。
         */
        if (ghost.getHuntTeleportCooldown() > 0) {
            return;
        }

        /*
         * ========================================
         * 当前目标
         * ========================================
         */
        LivingEntity target =
                ghost.getTarget();

        /*
         * 当前仍然拥有有效目标。
         *
         * 此时不应该继续累计“无目标时间”。
         */
        if (target != null
                && target.isAlive()
                && !target.isRemoved()
                && canContinueHuntingTarget(ghost, target)) {

            ghost.resetHuntNoTargetTicks();

            return;
        }

        /*
         * ========================================
         * 没有有效目标
         * ========================================
         *
         * 开始累计无目标时间。
         */
        ghost.addHuntNoTargetTick();

        /*
         * 尚未达到 30 秒，
         * 暂时不执行猎杀。
         */
        if (ghost.getHuntNoTargetTicks()
                < HUNT_NO_TARGET_TIME) {

            return;
        }

        /*
         * ========================================
         * 触发猎杀
         * ========================================
         *
         * 在真正寻找目标之前，
         * 先清零计时。
         *
         * 这样即使这次没有找到目标，
         * 也不会因为计时一直达到阈值而
         * 每 tick 重复执行搜索。
         */
        ghost.resetHuntNoTargetTicks();

        /*
         * ========================================
         * 寻找猎杀目标
         * ========================================
         */
        LivingEntity huntTarget =
                findHuntTarget(ghost);

        if (huntTarget == null) {

            QisPlan2.LOGGER.info(
                    "[夜游鬼猎杀] 夜游鬼 {} 等待了 30 秒，但附近没有找到可猎杀的实体",
                    ghost.getUUID()
            );

            return;
        }

        /*
         * ========================================
         * 寻找目标附近的黑暗位置
         * ========================================
         */
        BlockPos teleportPos =
                findHuntTeleportPosition(
                        ghost,
                        huntTarget
                );

        if (teleportPos == null) {

            QisPlan2.LOGGER.info(
                    "[夜游鬼猎杀] 夜游鬼 {} 找到了目标 {}，但目标附近没有合适的黑暗位置",
                    ghost.getUUID(),
                    huntTarget.getUUID()
            );

            /*
             * 即使没有找到合适的传送位置，
             * 仍然锁定这个目标。
             *
             * 这样夜游鬼至少会进入正常的
             * 目标追踪逻辑，而不是完全放弃目标。
             */
            ghost.setTarget(huntTarget);

            return;
        }

        /*
         * ========================================
         * 执行猎杀瞬移
         * ========================================
         */
        Vec3 teleportPosVec =
                Vec3.atBottomCenterOf(
                        teleportPos
                );

        ghost.teleportTo(
                teleportPosVec.x,
                teleportPosVec.y,
                teleportPosVec.z
        );

        /*
         * 瞬移后停止原来的导航。
         *
         * 否则旧导航路径可能继续存在，
         * 导致实体瞬移后出现不必要的移动。
         */
        ghost.getNavigation().stop();

        /*
         * ========================================
         * 锁定猎杀目标
         * ========================================
         */
        ghost.setTarget(huntTarget);

        /*
         * ========================================
         * 设置猎杀瞬移冷却
         * ========================================
         */
        ghost.setHuntTeleportCooldown(
                HUNT_TELEPORT_COOLDOWN
        );

        QisPlan2.LOGGER.info(
                "[夜游鬼猎杀] 夜游鬼 {} 强制猎杀目标 {}，"
                        + "目标是否处于黑暗：{}，"
                        + "传送位置：{}",
                ghost.getUUID(),
                huntTarget.getUUID(),
                isDarkEntity(
                        ghost,
                        huntTarget
                ),
                teleportPos
        );
    }

    /**
     * 判断夜游鬼是否已经解锁猎杀能力。
     */
    public static boolean hasHuntAbility(
            NightWanderer ghost
    ) {
        return ghost.getKillCount()
                > HUNT_UNLOCK_KILLS;
    }

    /**
     * 寻找猎杀目标。
     *
     * <p>
     * 目标选择规则：
     *
     * <ol>
     *     <li>优先选择处于黑暗中的实体。</li>
     *     <li>黑暗条件相同时，选择距离最近的实体。</li>
     *     <li>没有黑暗实体时，选择最近的普通实体。</li>
     * </ol>
     *
     * <p>
     * 创造模式和旁观模式玩家不会成为目标。
     *
     * @param ghost 当前夜游鬼
     * @return 找到的猎杀目标，没有则返回 {@code null}
     */
    private static LivingEntity findHuntTarget(
            NightWanderer ghost
    ) {

        AABB searchBox =
                ghost.getBoundingBox().inflate(
                        HUNT_TARGET_SEARCH_RANGE
                );

        List<LivingEntity> entities =
                ghost.level().getEntitiesOfClass(
                        LivingEntity.class,
                        searchBox,
                        entity -> {

                            /*
                             * ========================================
                             * 不能选择自己
                             * ========================================
                             */
                            if (entity == ghost) {
                                return false;
                            }

                            /*
                             * ========================================
                             * 必须仍然存活
                             * ========================================
                             */
                            if (!entity.isAlive()) {
                                return false;
                            }

                            /*
                             * 已经被移除的实体不能成为目标。
                             */
                            if (entity.isRemoved()) {
                                return false;
                            }

                            /*
                             * ========================================
                             * 玩家特殊处理
                             * ========================================
                             *
                             * 创造和旁观玩家不属于有效猎杀目标。
                             */
                            if (entity instanceof Player player) {
                                if (player.isCreative()
                                        || player.isSpectator()) {
                                    return false;
                                }

                                return ghost.canDetectPlayer(player);
                            }

                            /*
                             * 其他 LivingEntity 均允许成为目标。
                             */
                            return true;
                        }
                );

        if (entities.isEmpty()) {
            return null;
        }

        LivingEntity bestTarget = null;

        /*
         * 当前最佳目标是否处于黑暗。
         */
        boolean bestIsDark = false;

        /*
         * 当前最佳目标与夜游鬼之间的距离平方。
         *
         * 使用平方距离可以避免反复进行平方根计算。
         */
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity entity : entities) {

            boolean dark =
                    isDarkEntity(
                            ghost,
                            entity
                    );

            double distance =
                    ghost.distanceToSqr(entity);

            /*
             * ========================================
             * 第一个候选目标
             * ========================================
             */
            if (bestTarget == null) {

                bestTarget = entity;
                bestIsDark = dark;
                bestDistance = distance;

                continue;
            }

            /*
             * ========================================
             * 黑暗优先
             * ========================================
             *
             * 当前目标是黑暗，而原目标不是：
             * 直接替换。
             */
            if (dark && !bestIsDark) {

                bestTarget = entity;
                bestIsDark = true;
                bestDistance = distance;

                continue;
            }

            /*
             * 当前目标处于光亮，
             * 而原目标处于黑暗：
             *
             * 黑暗目标优先，因此不替换。
             */
            if (!dark && bestIsDark) {
                continue;
            }

            /*
             * ========================================
             * 光照条件相同
             * ========================================
             *
             * 距离更近的实体优先。
             */
            if (distance < bestDistance) {

                bestTarget = entity;
                bestIsDark = dark;
                bestDistance = distance;
            }
        }

        return bestTarget;
    }

    /**
     * 判断当前猎杀目标是否仍然符合夜游鬼的攻击规则。
     *
     * <p>
     * 目标可能在被锁定之后才进入白色鬼烛的保护状态，
     * 因此不能只在寻找目标时检查一次。
     */
    private static boolean canContinueHuntingTarget(
            NightWanderer ghost,
            LivingEntity target
    ) {
        if (target instanceof Player player) {
            return ghost.canDetectPlayer(player)
                    && ghost.canAttackPlayer(player);
        }

        return true;
    }

    /**
     * 判断实体是否处于黑暗环境。
     *
     * @param ghost 夜游鬼
     * @param entity 要检查的实体
     */
    private static boolean isDarkEntity(
            NightWanderer ghost,
            LivingEntity entity
    ) {
        return isDarkPosition(
                ghost,
                entity.blockPosition()
        );
    }

    /**
     * 判断一个位置是否属于夜游鬼认定的黑暗环境。
     *
     * <p>
     * 夜游鬼的黑暗判定规则：
     *
     * <ul>
     *     <li>方块光必须不高于 7。</li>
     *     <li>夜晚时不额外考虑天空光。</li>
     *     <li>白天时天空光也必须不高于 7。</li>
     * </ul>
     */
    private static boolean isDarkPosition(
            NightWanderer ghost,
            BlockPos pos
    ) {

        int blockLight =
                ghost.level().getBrightness(
                        LightLayer.BLOCK,
                        pos
                );

        /*
         * 方块光超过阈值，
         * 无论其他条件如何都不能算黑暗。
         */
        if (blockLight > HUNT_DARKNESS_THRESHOLD) {
            return false;
        }

        /*
         * 夜晚的露天空旷区域虽然存在天空光，
         * 但对于夜游鬼来说仍然属于可接受的黑暗环境。
         */
        if (ghost.level().isNight()) {
            return true;
        }

        /*
         * 白天必须进一步检查天空光。
         */
        int skyLight =
                ghost.level().getBrightness(
                        LightLayer.SKY,
                        pos
                );

        return skyLight <= HUNT_DARKNESS_THRESHOLD;
    }

    /**
     * 在目标附近寻找一个黑暗、安全的传送位置。
     *
     * <p>
     * 搜索范围为目标周围 {@value #HUNT_TELEPORT_RADIUS} 格。
     *
     * <p>
     * 搜索时还会检查目标上下数格，
     * 从而适应坑洞、楼梯以及其他高低差地形。
     *
     * @param ghost 当前夜游鬼
     * @param target 猎杀目标
     * @return 合适的传送位置，没有则返回 {@code null}
     */
    private static BlockPos findHuntTeleportPosition(
            NightWanderer ghost,
            LivingEntity target
    ) {

        BlockPos targetPos =
                target.blockPosition();

        BlockPos bestPosition = null;

        double bestDistance =
                Double.MAX_VALUE;

        int radius =
                Mth.ceil(
                        HUNT_TELEPORT_RADIUS
                );

        for (int x = -radius;
             x <= radius;
             x++) {

            for (int z = -radius;
                 z <= radius;
                 z++) {

                double horizontalDistanceSqr =
                        x * x + z * z;

                /*
                 * ========================================
                 * 与目标太近
                 * ========================================
                 *
                 * 防止夜游鬼直接与目标重叠。
                 */
                if (horizontalDistanceSqr
                        < HUNT_MIN_DISTANCE
                        * HUNT_MIN_DISTANCE) {

                    continue;
                }

                /*
                 * 超出最大水平搜索范围。
                 */
                if (horizontalDistanceSqr
                        > HUNT_TELEPORT_RADIUS
                        * HUNT_TELEPORT_RADIUS) {

                    continue;
                }

                int worldX =
                        targetPos.getX() + x;

                int worldZ =
                        targetPos.getZ() + z;

                /*
                 * ========================================
                 * 垂直方向搜索
                 * ========================================
                 *
                 * 目标上下各检查 3 格。
                 */
                for (int yOffset = -3;
                     yOffset <= 3;
                     yOffset++) {

                    int worldY =
                            targetPos.getY()
                                    + yOffset;

                    BlockPos candidate =
                            new BlockPos(
                                    worldX,
                                    worldY,
                                    worldZ
                            );

                    /*
                     * 检查这个位置是否适合作为
                     * 夜游鬼的传送位置。
                     */
                    if (!isValidHuntTeleportPosition(
                            ghost,
                            candidate,
                            target
                    )) {

                        continue;
                    }

                    double distanceSqr =
                            candidate.distSqr(
                                    targetPos
                            );

                    /*
                     * 在所有合格位置中，
                     * 选择距离目标最近的位置。
                     */
                    if (bestPosition == null
                            || distanceSqr
                            < bestDistance) {

                        bestPosition = candidate;
                        bestDistance = distanceSqr;
                    }
                }
            }
        }

        return bestPosition;
    }

    /**
     * 判断一个位置是否可以作为猎杀传送位置。
     *
     * <p>
     * 需要同时满足：
     *
     * <ul>
     *     <li>处于黑暗环境</li>
     *     <li>距离目标足够远</li>
     *     <li>脚下可以站立</li>
     *     <li>身体空间没有方块碰撞</li>
     *     <li>头部空间没有方块碰撞</li>
     *     <li>最终实体碰撞检测通过</li>
     * </ul>
     */
    private static boolean isValidHuntTeleportPosition(
            NightWanderer ghost,
            BlockPos pos,
            LivingEntity target
    ) {

        /*
         * ========================================
         * 黑暗检查
         * ========================================
         */
        if (!isDarkPosition(
                ghost,
                pos
        )) {
            return false;
        }

        /*
         * ========================================
         * 与目标的距离检查
         * ========================================
         */
        double distanceSqr =
                pos.distSqr(
                        target.blockPosition()
                );

        if (distanceSqr
                < HUNT_MIN_DISTANCE
                * HUNT_MIN_DISTANCE) {

            return false;
        }

        /*
         * ========================================
         * 脚下必须可以站立
         * ========================================
         *
         * isFaceSturdy() 比简单的 isSolid()
         * 更适合判断一个位置是否真的可以站立。
         */
        BlockPos groundPos =
                pos.below();

        if (!ghost.level().getBlockState(
                groundPos
        ).isFaceSturdy(
                ghost.level(),
                groundPos,
                net.minecraft.core.Direction.UP
        )) {

            return false;
        }

        /*
         * ========================================
         * 身体空间必须为空
         * ========================================
         */
        if (!ghost.level().getBlockState(
                pos
        ).getCollisionShape(
                ghost.level(),
                pos
        ).isEmpty()) {

            return false;
        }

        /*
         * ========================================
         * 头部空间必须为空
         * ========================================
         */
        BlockPos headPos =
                pos.above();

        if (!ghost.level().getBlockState(
                headPos
        ).getCollisionShape(
                ghost.level(),
                headPos
        ).isEmpty()) {

            return false;
        }

        /*
         * ========================================
         * 最终实体碰撞检查
         * ========================================
         *
         * 即使两个方块本身没有碰撞，
         * 夜游鬼的完整实体碰撞箱仍然可能
         * 与环境发生碰撞。
         *
         * 因此这里再进行一次最终验证。
         */
        Vec3 position =
                Vec3.atBottomCenterOf(pos);

        AABB movedBox =
                ghost.getBoundingBox().move(
                        position.x - ghost.getX(),
                        position.y - ghost.getY(),
                        position.z - ghost.getZ()
                );

        return ghost.level().noCollision(
                ghost,
                movedBox
        );
    }
}