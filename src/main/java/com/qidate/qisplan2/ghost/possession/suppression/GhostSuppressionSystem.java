package com.qidate.qisplan2.ghost.possession.suppression;

import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityRegistry;
import com.qidate.qisplan2.ghost.possession.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

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
                     * 只要目标拥有 ENTITY，
                     * 就属于鬼眼天然可以影响的对象。
                     */
                    new GhostSuppressionRule(
                            GhostTag.EYE,
                            GhostTag.ENTITY,
                            0.5D
                    )
            );

    /**
     * 判断 source 是否能够压制 target。
     */
    public static boolean canSuppress(
            PossessedGhostAbility source,
            PossessedGhostAbility target
    ) {

        GhostClassification sourceClassification =
                source.classification();

        GhostClassification targetClassification =
                target.classification();

        for (GhostSuppressionRule rule : RULES) {

            if (!sourceClassification.has(
                    rule.sourceTag()
            )) {
                continue;
            }

            if (!targetClassification.has(
                    rule.targetTag()
            )) {
                continue;
            }

            return true;
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
     * 压制由：
     *
     * <ul>
     *     <li>分类上的天然压制关系</li>
     *     <li>压制者当前灵异强度</li>
     *     <li>被压制者当前灵异强度</li>
     * </ul>
     *
     * 共同决定。
     */
    public static double getSuppression(
            ServerPlayer player,
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

            PossessedGhostAbility sourceAbility =
                    GhostAbilityRegistry.get(
                            sourceGhost
                    );

            if (sourceAbility == null) {
                continue;
            }

            /*
             * ========================================================
             * 查找分类上的压制关系
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
             * 获取双方当前实际灵异强度
             * ========================================================
             */

            double sourceStrength =
                    PossessionHandler.getEffectiveStrength(
                            player,
                            sourceGhost
                    );

            double targetStrength =
                    PossessionHandler.getEffectiveStrength(
                            player,
                            targetGhost
                    );

            if (sourceStrength <= 0.0D) {
                continue;
            }

            /*
             * ========================================================
             * 当前双方灵异强度对抗比例
             * ========================================================
             */

            double strengthRatio =
                    sourceStrength
                            / (
                            sourceStrength
                                    + targetStrength
                    );

            /*
             * ========================================================
             * 本次压制
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
             * 多个厉鬼同时压制时，
             * 使用剩余量继续计算。
             *
             * 这样不会简单相加超过 100%。
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
     * 获取 source 对 target 的分类压制强度。
     */
    private static double getRuleStrength(
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

            if (!sourceClassification.has(
                    rule.sourceTag()
            )) {
                continue;
            }

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