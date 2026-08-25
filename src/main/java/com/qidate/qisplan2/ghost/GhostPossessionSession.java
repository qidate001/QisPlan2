package com.qidate.qisplan2.ghost;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class GhostPossessionSession {

    public static final int TOTAL_TICKS = 20 * 20;

    public static final double MIN_SUCCESS = 0.0D;
    public static final double MAX_SUCCESS = 60.0D;

    public static final double CURSOR_SPEED = 0.025D;

    public static final double SUCCESS_GAIN = 1.2D;
    public static final double FAILURE_LOSS = 0.8D;

    /**
     * 玩家。
     */
    private final UUID playerUUID;

    /**
     * 被驾驭的鬼。
     */
    private final UUID ghostUUID;

    /**
     * 被驾驭的鬼实体 ID。
     */
    private final int ghostEntityId;

    /**
     * 玩家输入状态。
     */
    private boolean leftPressed;
    private boolean rightPressed;

    /**
     * 当前光标位置：
     *
     * 0.0 ~ 1.0
     */
    private double cursorPosition = 0.5D;

    /**
     * 当前判定点：
     *
     * 0.0 ~ 1.0
     */
    private double targetPosition = 0.5D;

    /**
     * 判定点移动速度。
     */
    private double targetVelocity = 0.015D;

    /**
     * 当前成功率。
     */
    private double success = 0.0D;

    /**
     * 剩余时间。
     */
    private int remainingTicks = TOTAL_TICKS;

    /**
     * 随机种子。
     */
    private final long randomSeed;

    public GhostPossessionSession(
            ServerPlayer player,
            UUID ghostUUID,
            int ghostEntityId,
            long randomSeed
    ) {
        this.playerUUID =
                player.getUUID();

        this.ghostUUID =
                ghostUUID;

        this.ghostEntityId =
                ghostEntityId;

        this.randomSeed =
                randomSeed;
    }

    public UUID playerUUID() {
        return playerUUID;
    }

    public UUID ghostUUID() {
        return ghostUUID;
    }

    public int ghostEntityId() {
        return ghostEntityId;
    }

    public double cursorPosition() {
        return cursorPosition;
    }

    public double targetPosition() {
        return targetPosition;
    }

    public double success() {
        return success;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    public void setLeftPressed(
            boolean pressed
    ) {
        leftPressed = pressed;
    }

    public void setRightPressed(
            boolean pressed
    ) {
        rightPressed = pressed;
    }

    public void tick() {

        /*
         * ========================================
         * 光标移动
         * ========================================
         */

        if (leftPressed && !rightPressed) {

            cursorPosition -= CURSOR_SPEED;

        } else if (rightPressed && !leftPressed) {

            cursorPosition += CURSOR_SPEED;
        }

        cursorPosition =
                Math.max(
                        0.0D,
                        Math.min(
                                1.0D,
                                cursorPosition
                        )
                );


        /*
         * ========================================
         * 判定点移动
         * ========================================
         */

        targetPosition +=
                targetVelocity;

        /*
         * 撞边反弹。
         */
        if (targetPosition <= 0.0D) {

            targetPosition = 0.0D;

            targetVelocity =
                    Math.abs(
                            targetVelocity
                    );

        } else if (targetPosition >= 1.0D) {

            targetPosition = 1.0D;

            targetVelocity =
                    -Math.abs(
                            targetVelocity
                    );
        }


        /*
         * ========================================
         * 剩余时间
         * ========================================
         */

        remainingTicks--;
    }


    /**
     * 判断光标是否覆盖判定点。
     */
    public boolean isAligned() {

        /*
         * 光标块有一定宽度。
         */
        double cursorWidth =
                0.08D;

        double targetWidth =
                0.055D;

        return Math.abs(
                cursorPosition
                        - targetPosition
        ) <= (
                cursorWidth
                        + targetWidth
        ) / 2.0D;
    }


    /**
     * 空格 / 下键的一次“用力”。
     */
    public void attempt() {

        if (isAligned()) {

            success +=
                    SUCCESS_GAIN;

        } else {

            success -=
                    FAILURE_LOSS;
        }

        success =
                Math.max(
                        MIN_SUCCESS,
                        Math.min(
                                MAX_SUCCESS,
                                success
                        )
                );
    }
}