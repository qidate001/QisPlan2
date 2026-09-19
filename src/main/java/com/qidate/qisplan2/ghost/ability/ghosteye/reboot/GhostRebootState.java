package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

public final class GhostRebootState {

    /**
     * 本次重启要回到的 Commit
     */
    private final int targetIndex;

    /**
     * 当前正在倒流的 Commit
     *
     * 例如：
     *
     * 当前在 #6
     * reverse #6 -> #5
     *
     * 那么 currentIndex = 6
     */
    private int currentIndex;

    /**
     * 距离下一次倒流还有多少 tick
     */
    private int tickCounter;

    public GhostRebootState(
            int targetIndex,
            int currentIndex
    ) {
        this.targetIndex = targetIndex;
        this.currentIndex = currentIndex;
        this.tickCounter = 0;
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