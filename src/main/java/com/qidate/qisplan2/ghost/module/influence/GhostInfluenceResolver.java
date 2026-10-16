package com.qidate.qisplan2.ghost.module.influence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 灵异影响统一裁决器。
 *
 * 只计算裁决结果，不应用游戏效果。
 */
public final class GhostInfluenceResolver {

    private GhostInfluenceResolver() {
    }

    /**
     * 裁决一批灵异影响。
     *
     * 当前规则：
     * 1. 只比较相同批次、相同目标、类型相反的影响。
     * 2. 同源、同强度：双方死机。
     * 3. 强度不同：较弱的一方被压制。
     * 4. 不同来源、相同强度：标记为待解决冲突。
     *
     * 注意：多条影响之间的复杂竞争规则仍需进一步完善。
     */
    public static List<GhostInfluenceResolution> resolve(
            List<GhostInfluence> influences
    ) {
        Objects.requireNonNull(influences, "influences");

        List<GhostInfluence> entries = List.copyOf(influences);

        List<GhostInfluenceResolution> results =
                new ArrayList<>(entries.size());

        for (int i = 0; i < entries.size(); i++) {
            results.add(GhostInfluenceResolution.ACCEPTED);
        }

        for (int i = 0; i < entries.size(); i++) {
            GhostInfluence first = entries.get(i);

            for (int j = i + 1; j < entries.size(); j++) {
                GhostInfluence second = entries.get(j);

                if (!sameConflictGroup(first, second)) {
                    continue;
                }

                GhostInfluenceResolution firstResult = results.get(i);
                GhostInfluenceResolution secondResult = results.get(j);

                // 已经确定死机的影响不再被后续比较覆盖。
                if (firstResult == GhostInfluenceResolution.DEADLOCKED
                        || secondResult == GhostInfluenceResolution.DEADLOCKED) {
                    continue;
                }

                int strengthComparison = Double.compare(
                        first.intensity(),
                        second.intensity()
                );

                if (strengthComparison == 0) {
                    if (first.sourceId().equals(second.sourceId())) {
                        results.set(
                                i,
                                GhostInfluenceResolution.DEADLOCKED
                        );
                        results.set(
                                j,
                                GhostInfluenceResolution.DEADLOCKED
                        );
                    } else {
                        results.set(
                                i,
                                GhostInfluenceResolution.CONTESTED
                        );
                        results.set(
                                j,
                                GhostInfluenceResolution.CONTESTED
                        );
                    }

                    continue;
                }

                if (strengthComparison > 0) {
                    if (secondResult
                            != GhostInfluenceResolution.DEADLOCKED) {
                        results.set(
                                j,
                                GhostInfluenceResolution.SUPPRESSED
                        );
                    }
                } else {
                    if (firstResult
                            != GhostInfluenceResolution.DEADLOCKED) {
                        results.set(
                                i,
                                GhostInfluenceResolution.SUPPRESSED
                        );
                    }
                }
            }
        }

        return List.copyOf(results);
    }

    private static boolean sameConflictGroup(
            GhostInfluence first,
            GhostInfluence second
    ) {
        return first.batchId() == second.batchId()
                && first.targetId().equals(second.targetId())
                && first.type() != second.type();
    }
}
