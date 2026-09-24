package com.qidate.qisplan2.ghost.possession.suppression;

import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.possession.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.GhostSuppressionAllocationHandler;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.ghost.possession.manager.SuppressionAllocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class GhostSuppressionSystem {

    private GhostSuppressionSystem() {
    }

    private static final List<GhostSuppressionRule> RULES =
            List.of(

                    /*
                     * ====================================================
                     * 鬼眼 → 实体鬼
                     * ====================================================
                     *
                     * 鬼眼的 EYE 灵异针对 ENTITY。
                     *
                     * 正向效率：90%
                     * 反向效率：45%
                     */
                    new GhostSuppressionRule(
                            GhostTag.EYE,
                            GhostTag.ENTITY,
                            0.9D,
                            0.45D
                    )
            );

    private record SuppressionResult(
            double forward,
            double reverse
    ) {
    }

    /**
     * 判断 source 是否能够压制 target。
     *
     * <p>
     * 来源鬼通过 suppressionTargets()
     * 声明自己能够压制哪些 Tag。
     *
     * <p>
     * 目标鬼通过 classification()
     * 声明自己的分类。
     */
    public static boolean canSuppress(
            PossessedGhostAbility source,
            PossessedGhostAbility target
    ) {

        if (source == null || target == null) {
            return false;
        }

        GhostClassification suppressionTargets =
                source.suppressionTargets();

        GhostClassification targetClassification =
                target.classification();

        for (GhostTag targetTag :
                suppressionTargets.tags()) {

            if (targetClassification.has(
                    targetTag
            )) {
                return true;
            }
        }

        return false;
    }

    /**
     * 计算一条实际灵异分配关系产生的双向压制。
     *
     * <p>
     * 一条 allocation：
     *
     * <pre>
     * source → target
     * </pre>
     *
     * 会同时产生两个方向的灵异限制：
     *
     * <ul>
     *     <li>forward：source 对 target 的压制</li>
     *     <li>reverse：target 对 source 的压制</li>
     * </ul>
     *
     * <p>
     * 注意：
     *
     * <strong>
     * 一条 allocation 只在这里计算一次。
     * </strong>
     */
    private static SuppressionResult calculateRelationshipSuppression(
            Player player,
            ResourceLocation sourceGhost,
            ResourceLocation targetGhost,
            PossessedGhostAbility sourceAbility,
            PossessedGhostAbility targetAbility,
            List<SuppressionAllocation> allocations
    ) {

        if (
                allocations == null
                        || allocations.isEmpty()
        ) {
            return new SuppressionResult(
                    0.0D,
                    0.0D
            );
        }

        /*
         * ================================================================
         * 来源鬼实际投入的灵异力量。
         *
         * 一个 slot =
         * suppressionUnitStrength()
         * ================================================================
         */
        double sourceStrength =
                allocations.size()
                        * sourceAbility
                        .suppressionUnitStrength();

        if (sourceStrength <= 0.0D) {
            return new SuppressionResult(
                    0.0D,
                    0.0D
            );
        }

        /*
         * ================================================================
         * 获取目标鬼自身的有效灵异强度。
         * ================================================================
         */
        double targetStrength =
                PossessionHandler.getEffectiveStrength(
                        player,
                        targetGhost
                );

        /*
         * ================================================================
         * 获取正向压制效率。
         *
         * source → target
         * ================================================================
         */

        if (!canSuppress(
                sourceAbility,
                targetAbility
        )) {
            return new SuppressionResult(
                    0.0D,
                    0.0D
            );
        }

        double forwardRuleStrength =
                getRuleStrength(
                        sourceAbility,
                        targetAbility
                );

        /*
         * ================================================================
         * 获取反向压制效率。
         *
         * 注意：
         *
         * 这里传入的仍然是原始关系：
         *
         * source → target
         *
         * reverseStrength 就属于这条规则。
         * ================================================================
         */
        double reverseRuleStrength =
                getReverseRuleStrength(
                        sourceAbility,
                        targetAbility
                );

        /*
         * ================================================================
         * 如果目标已经没有有效灵异力量：
         *
         * 正向视为完全压制。
         *
         * 反向没有力量，
         * 因此无法限制来源。
         * ================================================================
         */
        if (targetStrength <= 0.0D) {

            return new SuppressionResult(
                    forwardRuleStrength > 0.0D
                            ? 1.0D
                            : 0.0D,
                    0.0D
            );
        }

        /*
         * ================================================================
         * 双方参与这场灵异对抗的总力量。
         * ================================================================
         */
        double totalStrength =
                sourceStrength
                        + targetStrength;

        if (totalStrength <= 0.0D) {
            return new SuppressionResult(
                    0.0D,
                    0.0D
            );
        }

        /*
         * ================================================================
         * 来源方力量占比。
         *
         * source / (source + target)
         *
         * 例如：
         *
         * source = 100
         * target = 5
         *
         * = 95.24%
         * ================================================================
         */
        double sourceRatio =
                sourceStrength
                        / totalStrength;

        /*
         * ================================================================
         * 目标方力量占比。
         *
         * target / (source + target)
         *
         * 例如：
         *
         * target = 5
         * source = 100
         *
         * = 4.76%
         * ================================================================
         */
        double targetRatio =
                targetStrength
                        / totalStrength;

        /*
         * ================================================================
         * 正向压制：
         *
         * source → target
         * ================================================================
         */
        double forward =
                forwardRuleStrength
                        * sourceRatio;

        /*
         * ================================================================
         * 反向压制：
         *
         * target → source
         *
         * 使用 targetRatio。
         *
         * 这意味着：
         *
         * 目标越弱，
         * 反向限制来源就越弱。
         * ================================================================
         */
        double reverse =
                reverseRuleStrength
                        * targetRatio;

        return new SuppressionResult(
                Math.clamp(
                        forward,
                        0.0D,
                        1.0D
                ),
                Math.clamp(
                        reverse,
                        0.0D,
                        1.0D
                )
        );
    }

    /**
     * 获取某只鬼当前受到的压制程度。
     *
     * <p>
     * 每一条实际 allocation 都代表一条：
     *
     * <pre>
     * source → target
     * </pre>
     *
     * 的双向灵异关系。
     *
     * <p>
     * 这条关系只计算一次，
     * 然后：
     *
     * <ul>
     *     <li>查询 target：使用 forward</li>
     *     <li>查询 source：使用 reverse</li>
     * </ul>
     */
    public static double getSuppression(
            Player player,
            ResourceLocation targetGhost
    ) {

        if (
                player == null
                        || targetGhost == null
        ) {
            return 0.0D;
        }

        /*
         * ================================================================
         * 确认目标鬼存在。
         * ================================================================
         */
        PossessedGhostState targetState =
                PossessionHandler.getState(
                        player,
                        targetGhost
                );

        if (targetState == null) {
            return 0.0D;
        }

        PossessedGhostAbility targetAbility =
                GhostAbilityRegistry.get(
                        targetGhost
                );

        if (targetAbility == null) {
            return 0.0D;
        }

        /*
         * ================================================================
         * 剩余未被压制的灵异比例。
         *
         * 多条独立压制关系采用乘法叠加。
         * ================================================================
         */
        double remaining =
                1.0D;

        /*
         * ================================================================
         * 获取玩家当前全部灵异配平关系。
         *
         * 数据结构：
         *
         * sourceGhost
         *      ↓
         * targetGhost
         *      ↓
         * List<SuppressionAllocation>
         *
         * ================================================================
         */
        var allAllocations =
                GhostSuppressionAllocationHandler
                        .getAllocations(player);

        /*
         * ================================================================
         * 遍历每一条实际存在的 allocation 关系。
         * ================================================================
         */
        for (var sourceEntry :
                allAllocations.entrySet()) {

            ResourceLocation sourceGhost =
                    sourceEntry.getKey();

            PossessedGhostAbility sourceAbility =
                    GhostAbilityRegistry.get(
                            sourceGhost
                    );

            if (sourceAbility == null) {
                continue;
            }

            /*
             * ============================================================
             * source → target 的所有 slot 分配。
             * ============================================================
             */
            for (var targetEntry :
                    sourceEntry.getValue().entrySet()) {

                ResourceLocation allocatedTargetGhost =
                        targetEntry.getKey();

                /*
                 * ========================================================
                 * 当前查询的鬼如果既不是：
                 *
                 * source
                 *
                 * 也不是：
                 *
                 * target
                 *
                 * 那么这条关系与当前查询无关。
                 * ========================================================
                 */
                if (
                        !targetGhost.equals(
                                sourceGhost
                        )
                                && !targetGhost.equals(
                                allocatedTargetGhost
                        )
                ) {
                    continue;
                }

                PossessedGhostAbility allocatedTargetAbility =
                        GhostAbilityRegistry.get(
                                allocatedTargetGhost
                        );

                if (allocatedTargetAbility == null) {
                    continue;
                }

                List<SuppressionAllocation> allocations =
                        targetEntry.getValue();

                if (
                        allocations == null
                                || allocations.isEmpty()
                ) {
                    continue;
                }

                /*
                 * ========================================================
                 * --------------------------------------------------------
                 * 计算这一整条关系。
                 *
                 * source → allocatedTarget
                 *
                 * 一次同时得到：
                 *
                 * forward
                 * reverse
                 *
                 * --------------------------------------------------------
                 * ========================================================
                 */
                SuppressionResult result =
                        calculateRelationshipSuppression(
                                player,
                                sourceGhost,
                                allocatedTargetGhost,
                                sourceAbility,
                                allocatedTargetAbility,
                                allocations
                        );

                /*
                 * ========================================================
                 * 当前查询目标是 allocation 的 target。
                 *
                 * 例如：
                 *
                 * 查询：
                 * 开门鬼
                 *
                 * allocation：
                 * 鬼眼 → 开门鬼
                 *
                 * 使用：
                 * result.forward()
                 * ========================================================
                 */
                if (targetGhost.equals(
                        allocatedTargetGhost
                )) {

                    remaining *=
                            1.0D - result.forward();

                    continue;
                }

                /*
                 * ========================================================
                 * 当前查询目标是 allocation 的 source。
                 *
                 * 例如：
                 *
                 * 查询：
                 * 鬼眼
                 *
                 * allocation：
                 * 鬼眼 → 开门鬼
                 *
                 * 使用：
                 * result.reverse()
                 *
                 * 注意：
                 *
                 * 这里没有创建第二条 allocation。
                 *
                 * 它就是同一条：
                 *
                 * 鬼眼 → 开门鬼
                 *
                 * 所产生的反向灵异限制。
                 * ========================================================
                 */
                if (targetGhost.equals(
                        sourceGhost
                )) {

                    remaining *=
                            1.0D - result.reverse();
                }
            }
        }

        return Math.clamp(
                1.0D - remaining,
                0.0D,
                1.0D
        );
    }

    /**
     * 获取 source 对 target 的分类压制效率。
     */
    private static double getRuleStrength(
            PossessedGhostAbility source,
            PossessedGhostAbility target
    ) {

        GhostClassification sourceClassification =
                source.classification();

        GhostClassification suppressionTargets =
                source.suppressionTargets();

        GhostClassification targetClassification =
                target.classification();

        double result =
                0.0D;

        for (GhostSuppressionRule rule :
                RULES) {

            /*
             * 规则规定的来源 Tag，
             * 必须属于来源鬼自身分类。
             */
            if (!sourceClassification.has(
                    rule.sourceTag()
            )) {
                continue;
            }

            /*
             * 规则规定的目标 Tag，
             * 必须是来源鬼声明可以压制的 Tag。
             */
            if (!suppressionTargets.has(
                    rule.targetTag()
            )) {
                continue;
            }

            /*
             * 目标鬼必须拥有这个 Tag。
             */
            if (!targetClassification.has(
                    rule.targetTag()
            )) {
                continue;
            }

            result =
                    Math.max(
                            result,
                            rule.strength()
                    );
        }

        return result;
    }

    /**
     * 获取 source 对 target 的反向分类压制效率。
     *
     * <p>
     * 这里对应的是：
     *
     * <pre>
     * source → target
     * target → source
     * </pre>
     *
     * <p>
     * 反向压制不要求 target 自己声明
     * suppressionTargets()。
     *
     * <p>
     * 只要正向压制关系成立，
     * 就会根据规则产生对应的反向灵异限制。
     */
    private static double getReverseRuleStrength(
            PossessedGhostAbility source,
            PossessedGhostAbility target
    ) {

        GhostClassification sourceClassification =
                source.classification();

        GhostClassification targetClassification =
                target.classification();

        double result =
                0.0D;

        for (GhostSuppressionRule rule :
                RULES) {

            /*
             * ============================================================
             * 正向来源必须拥有 sourceTag。
             *
             * 例如：
             *
             * 鬼眼 = EYE
             * ============================================================
             */
            if (!sourceClassification.has(
                    rule.sourceTag()
            )) {
                continue;
            }

            /*
             * ============================================================
             * 正向目标必须拥有 targetTag。
             *
             * 例如：
             *
             * 敲门鬼 = ENTITY
             * ============================================================
             */
            if (!targetClassification.has(
                    rule.targetTag()
            )) {
                continue;
            }

            /*
             * ============================================================
             * 找到对应规则后，
             * 返回反向压制效率。
             *
             * 例如：
             *
             * EYE → ENTITY
             *
             * reverseStrength = 0.45
             * ============================================================
             */
            result =
                    Math.max(
                            result,
                            rule.reverseStrength()
                    );
        }

        return result;
    }
}