package com.qidate.qisplan2.ghost.ability.doorghost;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.PossessionHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 开门鬼 / 关门鬼驾驭后的能力处理器。
 *
 * 负责：
 *
 * 1. 门事件触发时，给附近的驾驭者标记触发者。
 * 2. 玩家按 G 时，寻找最近的标记目标并发动突袭。
 */
public final class DoorGhostAbilityHandler {

    private DoorGhostAbilityHandler() {
    }

    /*
     * ============================================================
     * 驾驭能力：门事件
     * ============================================================
     */

    /**
     * 当有人开门 / 关门时调用。
     *
     * 注意：
     *
     * 这里只负责“标记”。
     *
     * 真正的野生开门鬼 / 关门鬼攻击，
     * 仍然由 DoorGhostTriggerHandler 处理。
     */
    public static void onDoorChanged(
            ServerLevel level,
            BlockPos doorPos,
            LivingEntity source
    ) {

        if (source == null
                || !source.isAlive()) {

            return;
        }

        /*
         * ========================================================
         * 搜索范围
         * ========================================================
         */

        double centerX =
                doorPos.getX() + 0.5D;

        double centerY =
                doorPos.getY() + 0.5D;

        double centerZ =
                doorPos.getZ() + 0.5D;

        double radius =
                100.0D;

        double radiusSqr =
                radius * radius;


        /*
         * ========================================================
         * 100 格范围内所有玩家
         * ========================================================
         */

        for (ServerPlayer player :
                level.getEntitiesOfClass(
                        ServerPlayer.class,
                        new net.minecraft.world.phys.AABB(
                                centerX,
                                centerY,
                                centerZ,
                                centerX,
                                centerY,
                                centerZ
                        ).inflate(radius)
                )) {

            /*
             * ====================================================
             * 自己触发门事件：
             *
             * 不标记自己。
             *
             * 否则会给自己画 Outline。
             * ====================================================
             */

            if (player == source) {
                continue;
            }

            /*
             * ====================================================
             * 真正球形范围
             * ====================================================
             */

            if (player.distanceToSqr(
                    centerX,
                    centerY,
                    centerZ
            ) > radiusSqr) {

                continue;
            }


            boolean opening =
                    PossessionHandler.hasGhost(
                            player,
                            OpeningGhostAbility.ID
                    );

            boolean closing =
                    PossessionHandler.hasGhost(
                            player,
                            ClosingGhostAbility.ID
                    );


            QisPlan2.LOGGER.info(
                    "[QisPlan2] 玩家 {}：开门鬼={}，关门鬼={}",
                    player.getName().getString(),
                    opening,
                    closing
            );


            /*
             * ====================================================
             * 开门鬼
             * ====================================================
             */

            if (opening) {

                DoorGhostMarkManager.mark(
                        player,
                        source.getUUID()
                );
            }


            /*
             * ====================================================
             * 关门鬼
             * ====================================================
             */

            if (closing) {

                DoorGhostMarkManager.mark(
                        player,
                        source.getUUID()
                );
            }
        }
    }


    /*
     * ============================================================
     * G 键主动能力
     * ============================================================
     */

    /**
     * 玩家按下 G 后调用。
     *
     * 逻辑：
     *
     * 1. 判断是否驾驭开门鬼 / 关门鬼。
     * 2. 找最近的标记目标。
     * 3. 瞬移到目标旁边。
     * 4. 使用对应鬼的灵异袭击。
     */
    public static void use(
            ServerPlayer player
    ) {

        if (!(player.level()
                instanceof ServerLevel serverLevel)) {

            return;
        }


        /*
         * ========================================================
         * 判断是否驾驭门鬼
         * ========================================================
         */

        boolean opening =
                PossessionHandler.hasGhost(
                        player,
                        OpeningGhostAbility.ID
                );

        boolean closing =
                PossessionHandler.hasGhost(
                        player,
                        ClosingGhostAbility.ID
                );

        if (!opening && !closing) {
            return;
        }


        /*
         * ========================================================
         * 寻找最近的标记目标
         * ========================================================
         */

        LivingEntity target =
                DoorGhostMarkManager.findNearestMarkedTarget(
                        player,
                        serverLevel
                );

        if (target == null) {
            return;
        }


        /*
         * ========================================================
         * 找目标旁边的位置
         * ========================================================
         */

        BlockPos approach =
                findApproachPosition(
                        serverLevel,
                        target
                );

        if (approach == null) {
            return;
        }


        /*
         * ========================================================
         * 瞬移
         * ========================================================
         */

        player.teleportTo(
                approach.getX() + 0.5D,
                approach.getY(),
                approach.getZ() + 0.5D
        );


        /*
         * 玩家没有 Mob Navigation。
         *
         * 直接清掉速度。
         */
        player.setDeltaMovement(
                Vec3.ZERO
        );


        /*
         * ========================================================
         * 面向目标
         * ========================================================
         */

        faceTarget(
                player,
                target
        );


        /*
         * ========================================================
         * 发动对应鬼的袭击
         * ========================================================
         *
         * 如果同时驾驭：
         *
         * 暂时开门鬼优先。
         *
         * 后续再做真正的双鬼组合规则。
         */

        if (opening) {

            attackWithOpeningGhost(
                    player,
                    target
            );

        } else {

            attackWithClosingGhost(
                    player,
                    target
            );
        }
    }


    /*
     * ============================================================
     * 开门鬼攻击
     * ============================================================
     */

    private static void attackWithOpeningGhost(
            ServerPlayer player,
            LivingEntity target
    ) {

        double strength =
                PossessionHandler.getEffectiveStrength(
                        player,
                        OpeningGhostAbility.ID
                );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 开门鬼主动袭击：目标={}，强度={}",
                target.getName().getString(),
                strength
        );

        SupernaturalDeathHandler.tryKill(
                target,
                ModDamageTypes.openingGhost(
                        player
                ),
                strength
        );
    }


    /*
     * ============================================================
     * 关门鬼攻击
     * ============================================================
     */

    private static void attackWithClosingGhost(
            ServerPlayer player,
            LivingEntity target
    ) {

        double strength =
                PossessionHandler.getEffectiveStrength(
                        player,
                        ClosingGhostAbility.ID
                );

        QisPlan2.LOGGER.info(
                "[QisPlan2] 关门鬼主动袭击：目标={}，强度={}",
                target.getName().getString(),
                strength
        );

        SupernaturalDeathHandler.tryKill(
                target,
                ModDamageTypes.closingGhost(
                        player
                ),
                strength
        );
    }


    /*
     * ============================================================
     * 找目标附近可站立位置
     * ============================================================
     */

    private static BlockPos findApproachPosition(
            ServerLevel level,
            LivingEntity target
    ) {

        BlockPos center =
                target.blockPosition();

        BlockPos best =
                null;

        double bestDistance =
                Double.MAX_VALUE;


        /*
         * 四个水平面。
         */
        for (Direction direction :
                Direction.Plane.HORIZONTAL) {

            BlockPos candidate =
                    center.relative(
                            direction
                    );


            /*
             * ====================================================
             * 必须能站进去。
             * ====================================================
             */

            if (!level.getBlockState(
                    candidate
            ).getCollisionShape(
                    level,
                    candidate
            ).isEmpty()) {

                continue;
            }


            /*
             * ====================================================
             * 脚下必须有支撑。
             * ====================================================
             */

            BlockPos below =
                    candidate.below();

            if (level.getBlockState(
                    below
            ).getCollisionShape(
                    level,
                    below
            ).isEmpty()) {

                continue;
            }


            /*
             * ====================================================
             * 选距离目标最近的位置。
             * ====================================================
             */

            double distance =
                    target.distanceToSqr(
                            candidate.getX() + 0.5D,
                            candidate.getY(),
                            candidate.getZ() + 0.5D
                    );

            if (distance < bestDistance) {

                bestDistance =
                        distance;

                best =
                        candidate;
            }
        }


        /*
         * ========================================================
         * 找不到合适站位
         *
         * 直接用目标方块位置作为兜底。
         * ========================================================
         */

        if (best == null) {

            return center;
        }

        return best;
    }


    /*
     * ============================================================
     * 玩家朝向目标
     * ============================================================
     */

    private static void faceTarget(
            ServerPlayer player,
            LivingEntity target
    ) {

        double dx =
                target.getX()
                        - player.getX();

        double dy =
                target.getY()
                        + target.getBbHeight()
                        * 0.5D
                        - (
                        player.getY()
                                + player.getEyeHeight()
                );

        double dz =
                target.getZ()
                        - player.getZ();


        double horizontal =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );


        /*
         * Minecraft YRot：
         *
         * atan2(z, x) - 90°
         */
        float yRot =
                (float) (
                        Mth.atan2(
                                dz,
                                dx
                        )
                                * (
                                180.0D
                                        / Math.PI
                        )
                                - 90.0D
                );


        /*
         * Minecraft XRot：
         *
         * 向上为负。
         */
        float xRot =
                (float) (
                        -Mth.atan2(
                                dy,
                                horizontal
                        )
                                * (
                                180.0D
                                        / Math.PI
                        )
                );


        player.setYRot(
                yRot
        );

        player.setXRot(
                xRot
        );

        player.setYHeadRot(
                yRot
        );
    }
}