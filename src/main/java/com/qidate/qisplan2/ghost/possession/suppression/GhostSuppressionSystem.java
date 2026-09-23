package com.qidate.qisplan2.ghost.possession.suppression;

import com.qidate.qisplan2.ghost.possession.ability.PossessedGhostAbility;
import com.qidate.qisplan2.ghost.possession.classification.GhostClassification;
import com.qidate.qisplan2.ghost.possession.classification.GhostTag;

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
                            GhostTag.ENTITY
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
}