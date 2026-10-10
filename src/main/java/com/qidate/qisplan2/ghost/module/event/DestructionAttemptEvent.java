package com.qidate.qisplan2.ghost.module.event;

/**
 * 灵异语义事件：摧毁尝试。
 *
 * 事件默认允许摧毁。
 * 任意订阅此事件的鬼模块都可以撤销本次摧毁尝试。
 */
public abstract class DestructionAttemptEvent implements GhostEvent {

    private boolean canceled = false;

    /**
     * 撤销本次摧毁尝试。
     */
    public final void cancel() {
        this.canceled = true;
    }

    /**
     * 本次摧毁尝试是否已被撤销。
     */
    public final boolean isCanceled() {
        return canceled;
    }
}
