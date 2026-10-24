package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
                .computeIfAbsent(
                        host,
                        ignored -> new java.util.HashMap<>()
                )
                .computeIfAbsent(
                        moduleId,
                        ignored -> UUID.randomUUID()
                );
    }

    /**
     * 提交影响及其裁决通过后的效果。
     *
     * 默认没有额外的死机清理操作。
     */
    public void submit(
            GhostInfluence influence,
            Runnable acceptedEffect
    ) {
        submit(
                influence,
                acceptedEffect,
                () -> {
                }
        );
    }

    /**
     * 提交影响、获准效果以及死机清理操作。
     */
    public void submit(
            GhostInfluence influence,
            Runnable acceptedEffect,
            Runnable deadlockedEffect
    ) {
        Objects.requireNonNull(influence, "influence");
        Objects.requireNonNull(acceptedEffect, "acceptedEffect");
        Objects.requireNonNull(deadlockedEffect, "deadlockedEffect");

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
                        acceptedEffect,
                        deadlockedEffect
                )
        );
    }


    /**
     * 结束会话：
     * 1. 统一裁决；
     * 2. 应用死机状态；
     * 3. 执行死机影响的清理操作；
     * 4. 执行其余获准效果。
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

        // 第一阶段：先应用全部死机状态。
        int deadlockedCount = GhostInfluenceApplier.apply(
                influences,
                results
        );

        // 第二阶段：清理参与死机的具体影响。
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i) != GhostInfluenceResolution.DEADLOCKED) {
                continue;
            }

            submissions.get(i).executeDeadlockedEffect();
        }

        // 第三阶段：只执行仍然获准的效果。
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i) != GhostInfluenceResolution.ACCEPTED) {
                continue;
            }

            GhostInfluenceSubmission submission = submissions.get(i);
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

    /**
     * 用宿主对象身份与模块 ID 唯一标识一个模块实例。
     */
    private static final class DeadlockedModule {

        private final GhostModuleHost host;
        private final ResourceLocation moduleId;

        private DeadlockedModule(
                GhostModuleHost host,
                ResourceLocation moduleId
        ) {
            this.host = host;
            this.moduleId = moduleId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof DeadlockedModule other)) {
                return false;
            }

            return host == other.host
                    && moduleId.equals(other.moduleId);
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(host)
                    + moduleId.hashCode();
        }
    }
}