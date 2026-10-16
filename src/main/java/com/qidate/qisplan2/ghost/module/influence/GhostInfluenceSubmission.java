package com.qidate.qisplan2.ghost.module.influence;

import java.util.Objects;

/**
 * 一条待裁决的灵异影响及其后续效果。
 */
public record GhostInfluenceSubmission(
        GhostInfluence influence,
        Runnable acceptedEffect
) {

    public GhostInfluenceSubmission {
        Objects.requireNonNull(influence, "influence");
        Objects.requireNonNull(acceptedEffect, "acceptedEffect");
    }

    /**
     * 执行裁决通过后的效果。
     */
    public void executeAcceptedEffect() {
        acceptedEffect.run();
    }
}