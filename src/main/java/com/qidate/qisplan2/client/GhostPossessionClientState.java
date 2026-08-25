package com.qidate.qisplan2.client;

public final class GhostPossessionClientState {

    private static boolean active;

    private static int ghostEntityId;

    private static int remainingTicks;

    private static double cursorPosition;

    private static double targetPosition;

    private static double success;

    private GhostPossessionClientState() {
    }

    public static void start(
            int ghostEntityId,
            int totalTicks
    ) {
        active = true;

        GhostPossessionClientState.ghostEntityId =
                ghostEntityId;

        remainingTicks =
                totalTicks;

        cursorPosition =
                0.5D;

        targetPosition =
                0.5D;

        success =
                0.0D;
    }

    public static void update(
            int remainingTicks,
            double cursorPosition,
            double targetPosition,
            double success
    ) {
        GhostPossessionClientState.remainingTicks =
                remainingTicks;

        GhostPossessionClientState.cursorPosition =
                cursorPosition;

        GhostPossessionClientState.targetPosition =
                targetPosition;

        GhostPossessionClientState.success =
                success;
    }

    public static void end() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static int getGhostEntityId() {
        return ghostEntityId;
    }

    public static int getRemainingTicks() {
        return remainingTicks;
    }

    public static double getCursorPosition() {
        return cursorPosition;
    }

    public static double getTargetPosition() {
        return targetPosition;
    }

    public static double getSuccess() {
        return success;
    }
}