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
                     * 0.9 = 90% 灵异效率。
                     */
                    new GhostSuppressionRule(
                            GhostTag.EYE,
                            GhostTag.ENTITY,
                            0.9D
                    )
            );

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
     * 获取某只鬼当前受到的压制程度。
     *
     * <p>
     * 返回值范围：
     *
     * <ul>
     *     <li>0.0 = 没有压制</li>
     *     <li>1.0 = 完全压制</li>
     * </ul>
     *
     * <p>
     * 现在的压制来源不再是“整个来源鬼自动参与”。
     *
     * <p>
     * 只有玩家通过灵异配平系统，
     * 实际分配给目标鬼的 slot，
     * 才会转化为压制力量。
     */
    public static double getSuppression(
            Player player,
            ResourceLocation targetGhost
    ) {

        if (player == null || targetGhost == null) {
            return 0.0D;
        }

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

        double remaining =
                1.0D;

        /*
         * ================================================================
         * 遍历所有实际存在的压制分配。
         *
         * 不再遍历所有厉鬼然后自动计算。
         * ================================================================
         */
        for (ResourceLocation sourceGhost :
                PossessionHandler
                        .getAllStates(player)
                        .keySet()) {

            /*
             * 自己不能压制自己。
             */
            if (sourceGhost.equals(
                    targetGhost
            )) {
                continue;
            }

            /*
             * 获取来源鬼。
             */
            PossessedGhostAbility sourceAbility =
                    GhostAbilityRegistry.get(
                            sourceGhost
                    );

            if (sourceAbility == null) {
                continue;
            }

            /*
             * ========================================================
             * 只有存在实际分配时，
             * 来源鬼才参与这次压制。
             * ========================================================
             */
            List<SuppressionAllocation> allocations =
                    GhostSuppressionAllocationHandler
                            .getAllocations(
                                    player,
                                    sourceGhost,
                                    targetGhost
                            );

            if (
                    allocations == null
                            || allocations.isEmpty()
            ) {
                continue;
            }

            /*
             * ========================================================
             * Tag 是否允许压制？
             * ========================================================
             */
            if (!canSuppress(
                    sourceAbility,
                    targetAbility
            )) {
                continue;
            }

            /*
             * ========================================================
             * 获取 Tag 对应的压制效率。
             * ========================================================
             */
            double ruleStrength =
                    getRuleStrength(
                            sourceAbility,
                            targetAbility
                    );

            if (ruleStrength <= 0.0D) {
                continue;
            }

            /*
             * ========================================================
             * 计算这次实际分配出去的灵异力量。
             *
             * 一个 slot =
             * suppressionUnitStrength()
             *
             * 例如：
             *
             * 鬼眼：
             * 1 slot = 100
             * 3 slot = 300
             * ========================================================
             */
            double allocatedStrength =
                    allocations.size()
                            * sourceAbility
                            .suppressionUnitStrength();

            if (allocatedStrength <= 0.0D) {
                continue;
            }

            /*
             * ========================================================
             * 获取目标鬼当前实际灵异强度。
             * ========================================================
             */
            double targetStrength =
                    PossessionHandler.getEffectiveStrength(
                            player,
                            targetGhost
                    );

            if (targetStrength <= 0.0D) {
                /*
                 * 目标已经没有有效灵异力量。
                 *
                 * 此时直接视为完全压制。
                 */
                continue;
            }

            /*
             * ========================================================
             * 当前双方灵异力量对抗比例。
             *
             * 例如：
             *
             * 来源分配 = 100
             * 目标强度 = 200
             *
             * 100 / (100 + 200)
             * = 0.3333
             *
             * 再乘以 50% Tag 效率：
             *
             * 0.3333 × 0.5
             * = 0.1667
             *
             * 即约 16.67% 压制。
             * ========================================================
             */
            double strengthRatio =
                    allocatedStrength
                            / (
                            allocatedStrength
                                    + targetStrength
                    );

            /*
             * ========================================================
             * 本次压制。
             * ========================================================
             */
            double suppression =
                    ruleStrength
                            * strengthRatio;

            suppression =
                    Math.clamp(
                            suppression,
                            0.0D,
                            1.0D
                    );

            /*
             * ========================================================
             * 多个来源鬼同时压制：
             *
             * 使用剩余量继续计算，
             * 避免简单相加超过 100%。
             * ========================================================
             */
            remaining *=
                    1.0D - suppression;
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
}