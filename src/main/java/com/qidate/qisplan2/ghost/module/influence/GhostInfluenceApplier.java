package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.module.GhostModuleDataModifier;

import java.util.List;
import java.util.Objects;

/**
 * 灵异影响裁决结果应用器。
 *
 * 当前只应用死机结果，不执行保护或袭击效果。
 */
public final class GhostInfluenceApplier {

    private GhostInfluenceApplier() {
    }

    /**
     * 应用一批已经完成的裁决结果。
     *
     * @param influences 参与裁决的影响，顺序必须与 results 一致
     * @param results 对应的裁决结果
     * @return 成功设置死机状态的模块实例数量
     */
    public static int apply(
            List<GhostInfluence> influences,
            List<GhostInfluenceResolution> results
    ) {
        Objects.requireNonNull(influences, "influences");
        Objects.requireNonNull(results, "results");

        if (influences.size() != results.size()) {
            throw new IllegalArgumentException(
                    "Influences and results must have the same size"
            );
        }

        int deadlockedCount = 0;

        for (int i = 0; i < influences.size(); i++) {
            GhostInfluence influence = Objects.requireNonNull(
                    influences.get(i),
                    "influence"
            );

            GhostInfluenceResolution resolution = Objects.requireNonNull(
                    results.get(i),
                    "resolution"
            );

            if (resolution != GhostInfluenceResolution.DEADLOCKED) {
                continue;
            }

            if (!(influence.host() instanceof GhostModuleDataModifier modifier)) {
                continue;
            }

            if (!modifier.hasModule(influence.moduleId())) {
                continue;
            }

            if (modifier.setDeadlocked(influence.moduleId(), true)) {
                deadlockedCount++;
            }
        }

        return deadlockedCount;
    }
}