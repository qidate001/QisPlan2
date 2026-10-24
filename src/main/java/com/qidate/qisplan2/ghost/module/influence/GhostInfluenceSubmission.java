package com.qidate.qisplan2.ghost.module.influence;

import java.util.Objects;

/**
 * 一条待裁决的灵异影响及其后续效果。
 *
 * @param influence 对应的灵异影响
 * @param acceptedEffect 裁决通过后执行的效果
 * @param deadlockedEffect 该影响被判定为死机时执行的清理操作
 */
public record GhostInfluenceSubmission(
        GhostInfluence influence,
        Runnable acceptedEffect,
        Runnable deadlockedEffect
) {

    public GhostInfluenceSubmission {
        Objects.requireNonNull(influence, "influence");
        Objects.requireNonNull(acceptedEffect, "acceptedEffect");
        Objects.requireNonNull(deadlockedEffect, "deadlockedEffect");
    }

    /**
     * 兼容原有调用方式。
     *
     * 没有指定死机清理操作时，默认不执行任何操作。
     */
    public GhostInfluenceSubmission(
            GhostInfluence influence,
            Runnable acceptedEffect
    ) {
        this(
                influence,
                acceptedEffect,
                () -> {}
        );
    }

    /**
     * 执行裁决通过后的效果。
     */
    public void executeAcceptedEffect() {
        acceptedEffect.run();
    }

    /**
     * 执行该影响进入死机状态后的清理操作。
     */
    public void executeDeadlockedEffect() {
        deadlockedEffect.run();
    }
}