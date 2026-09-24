package com.qidate.qisplan2.ghost.possession.suppression;

import com.qidate.qisplan2.ghost.possession.classification.GhostTag;

public record GhostSuppressionRule(
        GhostTag sourceTag,
        GhostTag targetTag,
        double strength,
        double reverseStrength
) {
}