package com.qidate.qisplan2.ghost;

import net.minecraft.server.level.ServerPlayer;

import java.util.Random;
import java.util.UUID;

public final class GhostPossessionSession {

    public static final int TOTAL_TICKS = 20 * 20;

    public static final double MIN_SUCCESS = 0.0D;
    public static final double MAX_SUCCESS = 60.0D;

    public static final double CURSOR_SPEED = 0.025D;

    public static final double SUCCESS_GAIN = 1.2D;
    public static final double FAILURE_LOSS = 0.8D;

    private final UUID playerUUID;

    private final GhostPossessionTarget target;

    private boolean leftPressed;
    private boolean rightPressed;

    private double cursorPosition = 0.5D;

    private double targetPosition = 0.5D;

    private double targetDestination = 0.5D;

    private boolean targetMoving = false;

    private int targetPhaseTicks = 0;

    private double targetVelocity = 0.0D;

    private final Random random;

    private double success = 0.0D;

    private int remainingTicks = TOTAL_TICKS;

    private final long randomSeed;


    /*
     * ========================================================
     * 静态参数
     * ========================================================
     */

    private static final int TARGET_PAUSE_MIN = 8;

    private static final int TARGET_PAUSE_MAX = 28;

    private static final int TARGET_MOVE_MIN = 10;

    private static final int TARGET_MOVE_MAX = 35;

    private static final double TARGET_MOVE_DISTANCE_MIN = 0.08D;

    private static final double TARGET_MOVE_DISTANCE_MAX = 0.32D;

    private static final double TARGET_SPEED_MIN = 0.004D;

    private static final double TARGET_SPEED_MAX = 0.018D;


    public GhostPossessionSession(
            ServerPlayer player,
            GhostPossessionTarget target,
            long randomSeed
    ) {

        this.playerUUID =
                player.getUUID();

        this.target =
                target;

        this.randomSeed =
                randomSeed;

        this.random =
                new Random(
                        randomSeed
                );

        this.targetMoving =
                false;

        this.targetPhaseTicks =
                randomPauseTicks();
    }


    private int randomPauseTicks() {

        return TARGET_PAUSE_MIN
                + random.nextInt(
                TARGET_PAUSE_MAX
                        - TARGET_PAUSE_MIN
                        + 1
        );
    }


    private int randomMoveTicks() {

        return TARGET_MOVE_MIN
                + random.nextInt(
                TARGET_MOVE_MAX
                        - TARGET_MOVE_MIN
                        + 1
        );
    }


    private double randomRange(
            double min,
            double max
    ) {

        return min
                + random.nextDouble()
                * (max - min);
    }


    public UUID playerUUID() {
        return playerUUID;
    }


    public GhostPossessionTarget target() {
        return target;
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
                Math.clamp(
                        cursorPosition,
                        0.0D,
                        1.0D
                );


        /*
         * ========================================
         * 判定点移动
         * ========================================
         */

        tickTarget();


        /*
         * ========================================
         * 剩余时间
         * ========================================
         */

        remainingTicks--;
    }


    public boolean isAligned() {

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


    public void attempt() {

        if (isAligned()) {

            success +=
                    SUCCESS_GAIN;

        } else {

            success -=
                    FAILURE_LOSS;
        }

        success =
                Math.clamp(
                        success,
                        MIN_SUCCESS,
                        MAX_SUCCESS
                );
    }


    private void tickTarget() {

        if (!targetMoving) {

            targetPhaseTicks--;

            if (targetPhaseTicks <= 0) {

                startTargetMovement();
            }

            return;
        }


        targetPosition +=
                targetVelocity;


        boolean reached =
                targetVelocity > 0.0D
                        ? targetPosition
                        >= targetDestination
                        : targetPosition
                        <= targetDestination;


        if (reached) {

            targetPosition =
                    targetDestination;

            targetMoving =
                    false;

            targetPhaseTicks =
                    randomPauseTicks();

            targetVelocity =
                    0.0D;
        }
    }


    private void startTargetMovement() {

        boolean moveRight =
                random.nextBoolean();


        double distance =
                randomRange(
                        TARGET_MOVE_DISTANCE_MIN,
                        TARGET_MOVE_DISTANCE_MAX
                );


        double destination;

        if (moveRight) {

            destination =
                    targetPosition
                            + distance;

        } else {

            destination =
                    targetPosition
                            - distance;
        }


        destination =
                Math.clamp(
                        destination,
                        0.0D,
                        1.0D
                );


        if (Math.abs(
                destination
                        - targetPosition
        ) < TARGET_MOVE_DISTANCE_MIN / 2.0D) {

            destination =
                    moveRight
                            ? Math.max(
                            0.0D,
                            targetPosition
                                    - distance
                    )
                            : Math.min(
                            1.0D,
                            targetPosition
                                    + distance
                    );
        }


        targetDestination =
                destination;


        int moveTicks =
                randomMoveTicks();

        targetPhaseTicks =
                moveTicks;


        double totalDistance =
                Math.abs(
                        targetDestination
                                - targetPosition
                );


        if (totalDistance <= 0.0001D) {

            targetMoving =
                    false;

            targetPhaseTicks =
                    randomPauseTicks();

            targetVelocity =
                    0.0D;

            return;
        }


        targetVelocity =
                Math.copySign(
                        totalDistance
                                / moveTicks,
                        targetDestination
                                - targetPosition
                );


        double absVelocity =
                Math.abs(
                        targetVelocity
                );


        absVelocity =
                Math.clamp(
                        absVelocity,
                        TARGET_SPEED_MIN,
                        TARGET_SPEED_MAX
                );


        targetVelocity =
                Math.copySign(
                        absVelocity,
                        targetVelocity
                );


        targetMoving =
                true;
    }
}