package com.qidate.qisplan2.ghost.reboot;

public final class GhostRebootState {

    /**
     * 本次重启由谁发动。
     */
    private final GhostRebootSource source;

    /**
     * 最终要回到的 Commit。
     */
    private final int targetIndex;

    /**
     * 当前正在处理的 Commit。
     */
    private int currentIndex;

    /**
     * 距离下一次回溯还有多少 Tick。
     */
    private int tickCounter;

    public GhostRebootState(
            GhostRebootSource source,
            int targetIndex,
            int currentIndex
    ) {
        this.source = source;
        this.targetIndex = targetIndex;
        this.currentIndex = currentIndex;
        this.tickCounter = 0;
    }

    public GhostRebootSource getSource() {
        return source;
    }

    public int getTargetIndex() {
        return targetIndex;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(
            int currentIndex
    ) {
        this.currentIndex = currentIndex;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public void setTickCounter(
            int tickCounter
    ) {
        this.tickCounter = tickCounter;
    }

    public void tickCounterDown() {
        tickCounter--;
    }
}