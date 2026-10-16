package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/**
 * 一次待裁决的灵异影响。
 *
 * 只描述影响，不直接执行游戏效果。
 */
public record GhostInfluence(
        GhostModuleHost host,
        ResourceLocation moduleId,
        ResourceLocation ruleId,
        UUID targetId,
        GhostInfluenceType type,
        double intensity,
        UUID sourceId,
        long batchId
) {

    public GhostInfluence {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(sourceId, "sourceId");

        if (!host.hasModule(moduleId)) {
            throw new IllegalArgumentException(
                    "Host does not contain module: " + moduleId
            );
        }

        if (!Double.isFinite(intensity) || intensity < 0.0D) {
            throw new IllegalArgumentException(
                    "Influence intensity must be finite and non-negative"
            );
        }

        if (batchId < 0L) {
            throw new IllegalArgumentException(
                    "Batch ID must be non-negative"
            );
        }
    }
}