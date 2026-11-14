package com.qidate.qisplan2.ghost.possession.ability.ghostdoor;

import net.minecraft.resources.ResourceLocation;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.bearing.SpiritualPart;
import com.qidate.qisplan2.ghost.module.implementation.ClosingGhostModule;
import com.qidate.qisplan2.ghost.module.implementation.OpeningGhostModule;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 开门鬼 / 关门鬼驾驭后的能力处理器。
 *
 * 负责：
 *
 * 1. 门事件触发时，给附近的驾驭者标记触发者。
 * 2. 玩家按 G 时，寻找最近的标记目标并发动突袭。
 */
public final class GhostDoorAbilityHandler {
    /** 按选定模块追袭对应标记者，每次只调用一只门鬼。 */
    public static boolean use(ServerPlayer player, ResourceLocation moduleId) {
        boolean opening = moduleId.equals(OpeningGhostModule.ID);
        if ((!opening && !moduleId.equals(ClosingGhostModule.ID)) || !PossessionHandler.hasGhost(player, moduleId)) return false;
        LivingEntity target = GhostDoorMarkManager.findNearestMarkedTarget(player, player.serverLevel(),
                e -> opening ? GhostDoorMarkManager.hasOpeningMark(player, e.getUUID())
                             : GhostDoorMarkManager.hasClosingMark(player, e.getUUID()));
        if (target == null) return false;
        BlockPos approach = findApproachPosition(player.serverLevel(), target);
        if (approach == null) return false;
        player.teleportTo(approach.getX() + 0.5, approach.getY(), approach.getZ() + 0.5);
        player.setDeltaMovement(Vec3.ZERO);
        faceTarget(player, target);
        double strength = PossessionHandler.getEffectiveStrength(player, moduleId);
        if (opening) attackWithOpeningGhost(player, target, strength);
        else attackWithClosingGhost(player, target, strength);
        var state = PossessionHandler.getState(player, moduleId);
        if (state != null) PossessionHandler.setState(player, moduleId,
                PossessionHandler.addRevival(player, moduleId, state, 5, SpiritualPart.HAND));
        GhostDoorMarkManager.clearMarks(player, target.getUUID());
        return true;
    }

    /**
     * 驾驭后的开门鬼 / 关门鬼：
     *
     * 玩家主动开门 / 关门时，
     * 对附近生物发动灵异袭击的范围。
     */
    private static final double ACTIVE_ATTACK_RADIUS = 30.0D;


    private GhostDoorAbilityHandler() {
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
            LivingEntity source,
            boolean opening
    ) {

        if (source == null
                || !source.isAlive()) {

            return;
        }

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
         * 查找附近驾驭了门鬼的玩家。
         * ========================================================
         */

        for (ServerPlayer player :
                level.getEntitiesOfClass(
                        ServerPlayer.class,
                        new AABB(
                                centerX,
                                centerY,
                                centerZ,
                                centerX,
                                centerY,
                                centerZ
                        ).inflate(radius)
                )) {

            /*
             * 自己触发门事件时，不标记自己。
             */
            if (player == source) {
                continue;
            }

            /*
             * 球形范围。
             */
            if (player.distanceToSqr(
                    centerX,
                    centerY,
                    centerZ
            ) > radiusSqr) {

                continue;
            }

            /*
             * ====================================================
             * 开门
             * ====================================================
             */

            if (opening) {

                boolean hasOpeningGhost =
                        PossessionHandler.hasGhost(
                                player,
                                OpeningGhostModule.ID
                        );

                if (hasOpeningGhost) {

                    GhostDoorMarkManager.markOpening(
                            player,
                            source.getUUID()
                    );
                }

            }

            /*
             * ====================================================
             * 关门
             * ====================================================
             */

            else {

                boolean hasClosingGhost =
                        PossessionHandler.hasGhost(
                                player,
                                ClosingGhostModule.ID
                        );

                if (hasClosingGhost) {

                    GhostDoorMarkManager.markClosing(
                            player,
                            source.getUUID()
                    );
                }
            }
        }


        /*
         * ========================================================
         * 重点：
         *
         * 如果“开关门的人自己”就是门鬼驾驭者，
         * 那么发动主动能力。
         * ========================================================
         */

        if (!(source instanceof ServerPlayer player)) {
            return;
        }


        /*
         * ========================================================
         * 开门鬼
         * ========================================================
         */

        if (opening
                && PossessionHandler.hasGhost(
                player,
                OpeningGhostModule.ID
        )) {

            useOpeningGhostDoorAbility(
                    player,
                    level,
                    doorPos
            );

            return;
        }


        /*
         * ========================================================
         * 关门鬼
         * ========================================================
         */

        if (!opening
                && PossessionHandler.hasGhost(
                player,
                ClosingGhostModule.ID
        )) {

            useClosingGhostDoorAbility(
                    player,
                    level,
                    doorPos
            );
        }
    }

    private static void useOpeningGhostDoorAbility(
            ServerPlayer player,
            ServerLevel level,
            BlockPos doorPos
    ) {

        /*
         * ========================================================
         * 读取当前有效灵异强度。
         *
         * 注意：
         * 这里必须在增加复苏之前读取。
         * ========================================================
         */

        double attackStrength =
                PossessionHandler.getEffectiveStrength(
                        player,
                        OpeningGhostModule.ID
                );


        /*
         * ========================================================
         * 袭击附近所有生物。
         * ========================================================
         */

        int targetCount =
                attackNearbyLivingEntities(
                        player,
                        level,
                        doorPos,
                        attackStrength,
                        ModDamageTypes.openingGhost(
                                player
                        )
                );


        /*
         * ========================================================
         * 本次复苏：
         *
         * 基础 +10%
         * 每个目标 +2%
         * ========================================================
         */

        double revivalGain =
                10.0D
                        + targetCount * 2.0D;


        PossessedGhostState state =
                PossessionHandler.getState(
                        player,
                        OpeningGhostModule.ID
                );

        if (state == null) {
            return;
        }


        PossessedGhostState newState =
                PossessionHandler.addRevival(
                        player,
                        OpeningGhostModule.ID,
                        state,
                        revivalGain,
                        SpiritualPart.HAND
                );


        PossessionHandler.setState(
                player,
                OpeningGhostModule.ID,
                newState
        );
    }

    private static void useClosingGhostDoorAbility(
            ServerPlayer player,
            ServerLevel level,
            BlockPos doorPos
    ) {

        /*
         * ========================================================
         * 当前有效强度。
         * ========================================================
         */

        double attackStrength =
                PossessionHandler.getEffectiveStrength(
                        player,
                        ClosingGhostModule.ID
                );


        /*
         * ========================================================
         * 袭击附近所有生物。
         * ========================================================
         */

        int targetCount =
                attackNearbyLivingEntities(
                        player,
                        level,
                        doorPos,
                        attackStrength,
                        ModDamageTypes.closingGhost(
                                player
                        )
                );


        /*
         * ========================================================
         * 复苏：
         *
         * 基础 +10%
         * 每个目标 +2%
         * ========================================================
         */

        double revivalGain =
                10.0D
                        + targetCount * 2.0D;


        PossessedGhostState state =
                PossessionHandler.getState(
                        player,
                        ClosingGhostModule.ID
                );

        if (state == null) {
            return;
        }


        PossessedGhostState newState =
                PossessionHandler.addRevival(
                        player,
                        ClosingGhostModule.ID,
                        state,
                        revivalGain,
                        SpiritualPart.HAND
                );


        PossessionHandler.setState(
                player,
                ClosingGhostModule.ID,
                newState
        );
    }

    private static int attackNearbyLivingEntities(
            ServerPlayer player,
            ServerLevel level,
            BlockPos doorPos,
            double attackStrength,
            DamageSource damageSource
    ) {

        double radius =
                ACTIVE_ATTACK_RADIUS;

        double radiusSqr =
                radius * radius;

        AABB box =
                new AABB(
                        doorPos
                ).inflate(
                        radius
                );

        int targetCount = 0;

        /*
         * ========================================================
         * 查找 30 格内所有活着的生物。
         * ========================================================
         */

        for (LivingEntity entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        LivingEntity::isAlive
                )) {

            /*
             * ====================================================
             * 绝对不能袭击驾驭者自己。
             *
             * 同时使用对象判断 + UUID 判断。
             * ====================================================
             */

            if (entity == player
                    || entity.getUUID().equals(
                    player.getUUID()
            )) {

                continue;
            }

            /*
             * ====================================================
             * 计算真正的球形距离。
             * ====================================================
             */

            double dx =
                    entity.getX()
                            - (doorPos.getX() + 0.5D);

            double dy =
                    entity.getY()
                            - (doorPos.getY() + 0.5D);

            double dz =
                    entity.getZ()
                            - (doorPos.getZ() + 0.5D);

            double distanceSqr =
                    dx * dx
                            + dy * dy
                            + dz * dz;

            if (distanceSqr
                    > radiusSqr) {

                continue;
            }

            /*
             * ====================================================
             * 算作一个实际袭击目标。
             * ====================================================
             */

            targetCount++;

            SupernaturalDeathHandler.tryKill(
                    entity,
                    damageSource,
                    attackStrength
            );
        }

        return targetCount;
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


    private static void teleportToTarget(
            ServerLevel level,
            ServerPlayer player,
            LivingEntity target
    ) {

        BlockPos approach =
                findApproachPosition(
                        level,
                        target
                );

        if (approach == null) {
            return;
        }

        player.teleportTo(
                approach.getX() + 0.5D,
                approach.getY(),
                approach.getZ() + 0.5D
        );

        player.setDeltaMovement(
                Vec3.ZERO
        );

        faceTarget(
                player,
                target
        );
    }


    /*
     * ============================================================
     * 开门鬼攻击
     * ============================================================
     */

    private static void attackWithOpeningGhost(
            ServerPlayer player,
            LivingEntity target,
            double attackStrength
    ) {
        SupernaturalDeathHandler.tryKill(
                target,
                ModDamageTypes.openingGhost(
                        player
                ),
                attackStrength
        );
    }


    /*
     * ============================================================
     * 关门鬼攻击
     * ============================================================
     */

    private static void attackWithClosingGhost(
            ServerPlayer player,
            LivingEntity target,
            double attackStrength
    ) {
        SupernaturalDeathHandler.tryKill(
                target,
                ModDamageTypes.closingGhost(
                        player
                ),
                attackStrength
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