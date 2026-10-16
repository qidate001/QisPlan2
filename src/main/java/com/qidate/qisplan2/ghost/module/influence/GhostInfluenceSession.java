package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一次语义事件的灵异影响会话。
 *
 * 同一次事件中的所有模块共享同一个会话。
 * 会话结束前只收集影响，不执行提交的效果。
 */
public final class GhostInfluenceSession {

    private static final AtomicLong NEXT_BATCH_ID =
            new AtomicLong();

    private final GhostInfluenceBatch batch;

    private final List<GhostInfluenceSubmission> submissions =
            new ArrayList<>();

    private final Map<GhostModuleHost, Map<ResourceLocation, UUID>>
            sourceIds = new IdentityHashMap<>();

    private boolean finished;

    public GhostInfluenceSession() {
        this.batch = new GhostInfluenceBatch(nextBatchId());
    }

    public long getBatchId() {
        return batch.getBatchId();
    }

    /**
     * 获取本次事件中某个宿主模块实例的稳定来源标识。
     */
    public UUID getSourceId(
            GhostModuleHost host,
            ResourceLocation moduleId
    ) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(moduleId, "moduleId");

        if (finished) {
            throw new IllegalStateException(
                    "Influence session has finished"
            );
        }

        if (!host.hasModule(moduleId)) {
            throw new IllegalArgumentException(
                    "Host does not contain module: " + moduleId
            );
        }

        return sourceIds
                .computeIfAbsent(host, ignored -> new java.util.HashMap<>())
                .computeIfAbsent(moduleId, ignored -> UUID.randomUUID());
    }

    /**
     * 提交影响及其裁决通过后的效果。
     *
     * 本方法不会执行 acceptedEffect。
     */
    public void submit(
            GhostInfluence influence,
            Runnable acceptedEffect
    ) {
        Objects.requireNonNull(influence, "influence");
        Objects.requireNonNull(acceptedEffect, "acceptedEffect");

        if (finished) {
            throw new IllegalStateException(
                    "Cannot submit after the session has finished"
            );
        }

        if (influence.batchId() != batch.getBatchId()) {
            throw new IllegalArgumentException(
                    "Influence belongs to another batch"
            );
        }

        batch.submit(influence);

        submissions.add(
                new GhostInfluenceSubmission(
                        influence,
                        acceptedEffect
                )
        );
    }

    /**
     * 结束会话：统一裁决、应用死机状态，再执行获准效果。
     *
     * @return 成功设置死机状态的模块实例数量
     */
    public int resolveAndApply() {
        if (finished) {
            throw new IllegalStateException(
                    "Influence session has already finished"
            );
        }

        finished = true;

        List<GhostInfluence> influences = batch.getInfluences();
        List<GhostInfluenceResolution> results = batch.resolve();

        int deadlockedCount = GhostInfluenceApplier.apply(
                influences,
                results
        );

        for (int i = 0; i < results.size(); i++) {
            if (results.get(i) != GhostInfluenceResolution.ACCEPTED) {
                continue;
            }

            GhostInfluenceSubmission submission = submissions.get(i);

            // 模块可能已被之前的效果移除。
            GhostInfluence influence = submission.influence();

            if (!influence.host().hasModule(influence.moduleId())) {
                continue;
            }

            submission.executeAcceptedEffect();
        }

        return deadlockedCount;
    }

    public boolean isFinished() {
        return finished;
    }

    private static long nextBatchId() {
        long id = NEXT_BATCH_ID.getAndIncrement();

        if (id < 0L) {
            throw new IllegalStateException(
                    "Ghost influence batch ID exhausted"
            );
        }

        return id;
    }
}