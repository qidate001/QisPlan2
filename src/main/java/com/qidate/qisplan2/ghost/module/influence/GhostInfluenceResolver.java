package com.qidate.qisplan2.ghost.module.influence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 灵异影响统一裁决器。
 *
 * 按批次和目标分组，整体比较保护与袭击的最高强度。
 * 本类只计算裁决结果，不执行游戏效果。
 *
 * 裁决规则：
 * 1. 只有一种影响类型时，该类型的影响全部接受。
 * 2. 两种影响强度不同时，较弱类型的影响全部被压制。
 * 3. 两种影响最高强度相同时：
 *    - 同源的保护与袭击冲突，双方 DEADLOCKED。
 *    - 不同源的保护与袭击冲突，双方 CONTESTED。
 *    - 低于最高冲突强度的影响 SUPPRESSED。
 * 4. DEADLOCKED 优先于 CONTESTED，避免同一影响被后续冲突覆盖。
 */
public final class GhostInfluenceResolver {

    /**
     * 守护与袭击的强度比较容错比例。
     *
     * 最高强度差距不超过较大值的 7% 时，
     * 视为强度相当。
     */
    private static final double INTENSITY_TOLERANCE = 0.07D;

    private GhostInfluenceResolver() {
    }

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

        Map<ConflictKey, List<Integer>> groups = new HashMap<>();

        for (int i = 0; i < entries.size(); i++) {
            GhostInfluence influence = entries.get(i);

            ConflictKey key = new ConflictKey(
                    influence.batchId(),
                    influence.targetId()
            );

            groups.computeIfAbsent(
                    key,
                    ignored -> new ArrayList<>()
            ).add(i);
        }

        for (List<Integer> group : groups.values()) {
            resolveGroup(entries, results, group);
        }

        return List.copyOf(results);
    }

    private static void resolveGroup(
            List<GhostInfluence> entries,
            List<GhostInfluenceResolution> results,
            List<Integer> group
    ) {
        double maxProtection = -1.0D;
        double maxAttack = -1.0D;

        for (int index : group) {
            GhostInfluence influence = entries.get(index);

            if (influence.type() == GhostInfluenceType.PROTECTION) {
                maxProtection = Math.max(
                        maxProtection,
                        influence.intensity()
                );
            } else if (influence.type() == GhostInfluenceType.ATTACK) {
                maxAttack = Math.max(
                        maxAttack,
                        influence.intensity()
                );
            }
        }

        // 没有袭击，或没有保护，不存在对立冲突。
        if (maxAttack < 0.0D || maxProtection < 0.0D) {
            return;
        }

        if (!isIntensityEquivalent(maxProtection, maxAttack)) {
            if (maxProtection > maxAttack) {
                suppressType(
                        entries,
                        results,
                        group,
                        GhostInfluenceType.ATTACK
                );
            } else {
                suppressType(
                        entries,
                        results,
                        group,
                        GhostInfluenceType.PROTECTION
                );
            }

            return;
        }

        // 两种类型的最高强度相同。
        // 先压制所有低于最高冲突强度的影响。
        List<Integer> topProtection = new ArrayList<>();
        List<Integer> topAttack = new ArrayList<>();

        for (int index : group) {
            GhostInfluence influence = entries.get(index);

            if (influence.type() == GhostInfluenceType.PROTECTION) {
                if (Double.compare(
                        influence.intensity(),
                        maxProtection
                ) == 0) {
                    topProtection.add(index);
                } else {
                    results.set(
                            index,
                            GhostInfluenceResolution.SUPPRESSED
                    );
                }
            } else if (influence.type() == GhostInfluenceType.ATTACK) {
                if (Double.compare(
                        influence.intensity(),
                        maxAttack
                ) == 0) {
                    topAttack.add(index);
                } else {
                    results.set(
                            index,
                            GhostInfluenceResolution.SUPPRESSED
                    );
                }
            }
        }

        // 记录哪些最高强度影响参与了同源冲突。
        boolean[] deadlocked = new boolean[entries.size()];

        for (int protectionIndex : topProtection) {
            GhostInfluence protection = entries.get(protectionIndex);

            for (int attackIndex : topAttack) {
                GhostInfluence attack = entries.get(attackIndex);

                if (protection.sourceId().equals(attack.sourceId())) {
                    deadlocked[protectionIndex] = true;
                    deadlocked[attackIndex] = true;
                }
            }
        }

        // 先统一写入 DEADLOCKED。
        for (int index : topProtection) {
            if (deadlocked[index]) {
                results.set(
                        index,
                        GhostInfluenceResolution.DEADLOCKED
                );
            }
        }

        for (int index : topAttack) {
            if (deadlocked[index]) {
                results.set(
                        index,
                        GhostInfluenceResolution.DEADLOCKED
                );
            }
        }

        // 没有参与同源冲突的最高强度影响，属于未解决的不同源冲突。
        // 不执行其保护或袭击效果。
        for (int index : topProtection) {
            if (!deadlocked[index]) {
                results.set(
                        index,
                        GhostInfluenceResolution.CONTESTED
                );
            }
        }

        for (int index : topAttack) {
            if (!deadlocked[index]) {
                results.set(
                        index,
                        GhostInfluenceResolution.CONTESTED
                );
            }
        }
    }

    private static void suppressType(
            List<GhostInfluence> entries,
            List<GhostInfluenceResolution> results,
            List<Integer> group,
            GhostInfluenceType losingType
    ) {
        for (int index : group) {
            if (entries.get(index).type() == losingType) {
                results.set(
                        index,
                        GhostInfluenceResolution.SUPPRESSED
                );
            }
        }
    }

    /**
     * 判断两种最高强度是否处于容错范围内。
     */
    private static boolean isIntensityEquivalent(
            double protection,
            double attack
    ) {
        double max = Math.max(protection, attack);

        if (max == 0.0D) {
            return true;
        }

        return Math.abs(protection - attack)
                / max <= INTENSITY_TOLERANCE;
    }

    private record ConflictKey(long batchId, UUID targetId) {
    }
}