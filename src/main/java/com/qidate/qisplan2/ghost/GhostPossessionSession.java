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

    private double targetPosition = 0.5D;

    /**
     * 当前这一段移动的目标位置。
     */
    private double targetDestination = 0.5D;

    /**
     * 当前是否正在移动。
     */
    private boolean targetMoving = false;

    /**
     * 当前阶段剩余 tick。
     *
     * 移动时：
     *     表示还允许移动多久。
     *
     * 停顿时：
     *     表示还要停多久。
     */
    private int targetPhaseTicks = 0;

    /**
     * 当前移动速度。
     */
    private double targetVelocity = 0.0D;

    private final Random random;

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


    /*
     * 静态参数
     */

    /**
     * 最短停顿。
     */
    private static final int TARGET_PAUSE_MIN = 8;

    /**
     * 最长停顿。
     */
    private static final int TARGET_PAUSE_MAX = 28;

    /**
     * 最短移动时间。
     */
    private static final int TARGET_MOVE_MIN = 10;

    /**
     * 最长移动时间。
     */
    private static final int TARGET_MOVE_MAX = 35;

    /**
     * 判定点一次移动的最小距离。
     */
    private static final double TARGET_MOVE_DISTANCE_MIN = 0.08D;

    /**
     * 判定点一次移动的最大距离。
     */
    private static final double TARGET_MOVE_DISTANCE_MAX = 0.32D;

    /**
     * 最低移动速度。
     */
    private static final double TARGET_SPEED_MIN = 0.004D;

    /**
     * 最高移动速度。
     */
    private static final double TARGET_SPEED_MAX = 0.018D;



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

        this.random =
                new Random(
                        randomSeed
                );

        /*
         * 开局先停一会儿。
         */
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
        tickTarget();

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

    private void tickTarget() {

        /*
         * ========================================================
         * 当前正在停顿
         * ========================================================
         */
        if (!targetMoving) {

            targetPhaseTicks--;

            if (targetPhaseTicks <= 0) {

                startTargetMovement();
            }

            return;
        }


        /*
         * ========================================================
         * 当前正在移动
         * ========================================================
         */
        targetPosition +=
                targetVelocity;

        /*
         * 到达目标位置。
         */
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

            /*
             * 到达以后停一会。
             */
            targetPhaseTicks =
                    randomPauseTicks();

            targetVelocity = 0.0D;
        }
    }

    private void startTargetMovement() {

        /*
         * ========================================================
         * 决定移动方向
         * ========================================================
         */

        boolean moveRight =
                random.nextBoolean();

        /*
         * ========================================================
         * 随机移动距离
         * ========================================================
         */
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

        /*
         * 防止越界。
         */
        destination =
                Math.max(
                        0.0D,
                        Math.min(
                                1.0D,
                                destination
                        )
                );

        /*
         * 如果因为靠近边缘，
         * 实际上没移动多少，
         * 就反向尝试一次。
         */
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

        /*
         * ========================================================
         * 移动时间
         * ========================================================
         */
        int moveTicks =
                randomMoveTicks();

        targetPhaseTicks =
                moveTicks;

        /*
         * ========================================================
         * 速度
         * ========================================================
         */
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

        /*
         * 限制一下速度范围，
         * 防止太慢或者突然飞过去。
         */
        double absVelocity =
                Math.abs(
                        targetVelocity
                );

        absVelocity =
                Math.max(
                        TARGET_SPEED_MIN,
                        Math.min(
                                TARGET_SPEED_MAX,
                                absVelocity
                        )
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