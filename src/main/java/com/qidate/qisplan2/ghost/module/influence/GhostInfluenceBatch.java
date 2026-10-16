package com.qidate.qisplan2.ghost.module.influence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 一批等待统一裁决的灵异影响。
 */
public final class GhostInfluenceBatch {

    private final long batchId;

    private final List<GhostInfluence> influences =
            new ArrayList<>();

    private boolean resolved;

    public GhostInfluenceBatch(long batchId) {
        if (batchId < 0L) {
            throw new IllegalArgumentException(
                    "Batch ID must be non-negative"
            );
        }

        this.batchId = batchId;
    }

    public long getBatchId() {
        return batchId;
    }

    public void submit(GhostInfluence influence) {
        Objects.requireNonNull(influence, "influence");

        if (resolved) {
            throw new IllegalStateException(
                    "Cannot submit influence after resolution"
            );
        }

        if (influence.batchId() != batchId) {
            throw new IllegalArgumentException(
                    "Influence belongs to another batch"
            );
        }

        influences.add(influence);
    }

    public List<GhostInfluence> getInfluences() {
        return List.copyOf(influences);
    }

    /**
     * 只计算裁决结果，不应用游戏效果。
     */
    public List<GhostInfluenceResolution> resolve() {
        if (resolved) {
            throw new IllegalStateException(
                    "Batch has already been resolved"
            );
        }

        List<GhostInfluenceResolution> results =
                GhostInfluenceResolver.resolve(influences);

        resolved = true;

        return results;
    }

    /**
     * 裁决并应用当前支持的结果。
     *
     * 当前只应用 DEADLOCKED，不执行保护或袭击效果。
     *
     * @return 成功设置死机状态的模块实例数量
     */
    public int resolveAndApply() {
        List<GhostInfluenceResolution> results = resolve();

        return GhostInfluenceApplier.apply(
                getInfluences(),
                results
        );
    }

    public boolean isResolved() {
        return resolved;
    }
}