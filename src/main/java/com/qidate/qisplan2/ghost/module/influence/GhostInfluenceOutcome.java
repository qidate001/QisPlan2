package com.qidate.qisplan2.ghost.module.influence;

/** 完整强度裁决结果。剩余强度属于本次影响，不修改厉鬼本身的强度。 */
public record GhostInfluenceOutcome(GhostInfluenceResolution status, double originalIntensity,
                                    double effectiveIntensity) {
    /** 本次对抗中消耗或被压制的强度。 */
    public double absorbedIntensity() { return originalIntensity - effectiveIntensity; }
}
