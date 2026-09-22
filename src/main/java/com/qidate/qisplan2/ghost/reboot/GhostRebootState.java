package com.qidate.qisplan2.ghost.reboot;

public final class GhostRebootState {

    private final GhostRebootSource source;

    private int targetIndex;

    private int currentIndex;

    private int tickCounter;

    /*
     * 是否持续重启。
     *
     * true：
     * 当前这一轮结束后，会再次从当前时间倒流到目标 Commit。
     *
     * false：
     * 当前这一轮结束后，重启彻底结束。
     */
    private boolean continuous;

    public GhostRebootState(
            GhostRebootSource source,
            int targetIndex,
            int currentIndex,
            boolean continuous
    ) {
        this.source = source;
        this.targetIndex = targetIndex;
        this.currentIndex = currentIndex;
        this.continuous = continuous;
        this.tickCounter = 0;
    }

    public GhostRebootSource getSource() {
        return source;
    }

    public int getTargetIndex() {
        return targetIndex;
    }

    public void setTargetIndex(int targetIndex) {
        this.targetIndex = targetIndex;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public void setTickCounter(int tickCounter) {
        this.tickCounter = tickCounter;
    }

    public void tickCounterDown() {
        tickCounter--;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }
}