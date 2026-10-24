package com.qidate.qisplan2.ghost.module.influence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 灵异影响统一裁决器。
 *
 * 按批次和目标分组，仅对已注册为互斥关系的影响类型进行裁决。
 *
 * PROTECTION 与 ATTACK 采用逐层抵挡：
 * 1. 防护按强度从高到低排列。
 * 2. 袭击依次面对防护。
 * 3. 袭击强于防护时，防护被击穿，袭击保留剩余强度。
 * 4. 防护强于袭击时，袭击被抵挡，防护保留剩余强度。
 * 5. 强度相当时，同源导致死机，异源进入争夺。
 * 6. 死机立即终止该目标本批次的后续攻防计算。
 *
 * 本类只计算裁决结果，不执行游戏效果。
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
        List<Integer> protections = new ArrayList<>();
        List<Integer> attacks = new ArrayList<>();

        for (int index : group) {
            GhostInfluence influence = entries.get(index);

            if (influence.type() == GhostInfluenceType.PROTECTION) {
                protections.add(index);
            } else if (influence.type() == GhostInfluenceType.ATTACK) {
                attacks.add(index);
            }
        }

        if (protections.isEmpty() || attacks.isEmpty()) {
            return;
        }

        // 当前按防护原始强度从高到低排序。
        // 相同强度时使用输入列表中的先后顺序，保证结果稳定。
        protections.sort(
                Comparator
                        .<Integer>comparingDouble(
                                index -> entries.get(index).intensity()
                        )
                        .reversed()
                        .thenComparingInt(Integer::intValue)
        );

        // 每条防护维护独立的剩余强度。
        Map<Integer, Double> remainingProtection = new HashMap<>();

        for (int index : protections) {
            remainingProtection.put(
                    index,
                    entries.get(index).intensity()
            );
        }

        // 每条袭击按输入顺序处理。
        // 每条袭击依次面对从高到低排列的防护。
        for (int attackIndex : attacks) {
            GhostInfluence attack = entries.get(attackIndex);
            double remainingAttack = attack.intensity();

            boolean attackStopped = false;

            for (int protectionIndex : protections) {
                if (attackStopped) {
                    break;
                }

                double defenseStrength =
                        remainingProtection.get(protectionIndex);

                if (defenseStrength <= 0.0D) {
                    continue;
                }

                GhostInfluence protection =
                        entries.get(protectionIndex);

                if (remainingAttack <= 0.0D) {
                    break;
                }

                if (isIntensityEquivalent(
                        remainingAttack,
                        defenseStrength
                )) {
                    if (protection.sourceId().equals(
                            attack.sourceId()
                    )) {
                        results.set(
                                protectionIndex,
                                GhostInfluenceResolution.DEADLOCKED
                        );

                        results.set(
                                attackIndex,
                                GhostInfluenceResolution.DEADLOCKED
                        );

                        // 同源死机：立即停止该目标本批次的后续攻防。
                        return;
                    }

                    results.set(
                            protectionIndex,
                            GhostInfluenceResolution.CONTESTED
                    );

                    results.set(
                            attackIndex,
                            GhostInfluenceResolution.CONTESTED
                    );

                    remainingProtection.put(protectionIndex, 0.0D);

                    attackStopped = true;
                    break;
                }

                if (remainingAttack > defenseStrength) {
                    // 袭击击穿当前防护，保留剩余袭击强度。
                    remainingAttack -= defenseStrength;

                    remainingProtection.put(
                            protectionIndex,
                            0.0D
                    );

                    results.set(
                            protectionIndex,
                            GhostInfluenceResolution.SUPPRESSED
                    );
                } else {
                    // 当前防护抵挡住袭击，防护保留剩余强度。
                    double remainingDefense =
                            defenseStrength - remainingAttack;

                    remainingProtection.put(
                            protectionIndex,
                            remainingDefense
                    );

                    results.set(
                            attackIndex,
                            GhostInfluenceResolution.SUPPRESSED
                    );

                    attackStopped = true;
                }
            }

            if (!attackStopped && remainingAttack > 0.0D) {
                // 所有防护均被击穿，袭击仍有剩余强度。
                results.set(
                        attackIndex,
                        GhostInfluenceResolution.ACCEPTED
                );
            }
        }
    }

    /**
     * 判断两种最高强度是否处于容错范围内。
     */
    private static boolean isIntensityEquivalent(
            double first,
            double second
    ) {
        double max = Math.max(first, second);

        if (max == 0.0D) {
            return true;
        }

        return Math.abs(first - second) / max
                <= INTENSITY_TOLERANCE;
    }

    private record ConflictKey(long batchId, UUID targetId) {
    }
}