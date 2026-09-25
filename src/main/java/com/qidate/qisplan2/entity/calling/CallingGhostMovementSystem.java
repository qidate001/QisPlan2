package com.qidate.qisplan2.entity.calling;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 管理喊人鬼的移动与玩家转身检测。
 *
 * <p>
 * 负责：
 * <ul>
 *     <li>跟随当前目标玩家</li>
 *     <li>维持与玩家之间的固定距离</li>
 *     <li>维持跟随高度</li>
 *     <li>记录玩家上一刻的朝向</li>
 *     <li>检测玩家是否突然回头</li>
 * </ul>
 *
 * <p>
 * 这里不负责目标选择、喊名以及回头后的灵异攻击。
 */
public final class CallingGhostMovementSystem {

    private static final double FOLLOW_DISTANCE = 2.5D;
    private static final double FOLLOW_HEIGHT = 1.0D;
    private static final float TURN_THRESHOLD = 45.0F;

    private CallingGhostMovementSystem() {
    }

    /**
     * 初始化玩家朝向记录。
     *
     * <p>
     * 当喊人鬼刚获得一个新的目标时，
     * 不应该把玩家当前朝向误判为“回头”。
     */
    public static void startFollowing(
            CallingGhost ghost,
            ServerPlayer player
    ) {
        ghost.lastPlayerYRot = player.getYRot();
        ghost.hasLastPlayerRotation = true;
    }

    /**
     * 清除当前跟随目标的朝向记录。
     */
    public static void stopFollowing(
            CallingGhost ghost
    ) {
        ghost.hasLastPlayerRotation = false;
    }

    /**
     * 每 tick 更新移动与回头检测。
     *
     * @return 玩家是否在本 tick 回头
     */
    public static boolean tick(
            CallingGhost ghost,
            ServerPlayer player
    ) {
        if (hasPlayerTurnedAround(ghost, player)) {
            return true;
        }

        ghost.lastPlayerYRot = player.getYRot();
        ghost.hasLastPlayerRotation = true;

        followPlayer(ghost, player);

        return false;
    }

    /**
     * 检测玩家是否在本 tick 内转身超过阈值。
     */
    private static boolean hasPlayerTurnedAround(
            CallingGhost ghost,
            ServerPlayer player
    ) {
        if (!ghost.hasLastPlayerRotation) {
            return false;
        }

        float currentYRot = player.getYRot();

        float rotationDelta =
                Math.abs(
                        Mth.wrapDegrees(
                                currentYRot
                                        - ghost.lastPlayerYRot
                        )
                );

        return rotationDelta >= TURN_THRESHOLD;
    }

    /**
     * 让喊人鬼保持在玩家身后一定距离和高度。
     */
    private static void followPlayer(
            CallingGhost ghost,
            ServerPlayer player
    ) {
        Vec3 playerLook =
                player.getLookAngle();

        Vec3 targetPosition =
                player.position()
                        .subtract(
                                playerLook.x * FOLLOW_DISTANCE,
                                0.0D,
                                playerLook.z * FOLLOW_DISTANCE
                        )
                        .add(
                                0.0D,
                                FOLLOW_HEIGHT,
                                0.0D
                        );

        Vec3 movement =
                targetPosition.subtract(
                        ghost.position()
                );

        double distance =
                movement.length();

        if (distance < 0.1D) {
            ghost.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 direction =
                movement.normalize();

        double speed =
                Math.min(
                        distance * 0.5D,
                        0.35D
                );

        ghost.setDeltaMovement(
                direction.scale(speed)
        );

        ghost.setYRot(
                player.getYRot()
        );

        ghost.setYBodyRot(
                player.getYRot()
        );
    }
}