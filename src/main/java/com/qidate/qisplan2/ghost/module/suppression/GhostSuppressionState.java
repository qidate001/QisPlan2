package com.qidate.qisplan2.ghost.module.suppression;

/** 分别缓存外来与体内压制，不让体内豁免覆盖外来关系。 */
public record GhostSuppressionState(GhostSuppression.Level external, GhostSuppression.Level internal, double pressure) {
    /** 兼容只表达权限的状态；无强度信息时入睡速度采用基础值。 */
    public GhostSuppressionState(GhostSuppression.Level external, GhostSuppression.Level internal) {
        this(external, internal, 0);
    }
    public static final GhostSuppressionState NONE = new GhostSuppressionState(
            GhostSuppression.Level.NONE, GhostSuppression.Level.NONE);
    public static final GhostSuppressionState EXTERNAL_COMPLETE = new GhostSuppressionState(
            GhostSuppression.Level.COMPLETE, GhostSuppression.Level.NONE, Double.POSITIVE_INFINITY);
    /** 不同媒介来源取最强限制；保留体内/外来区别，不能用体内豁免覆盖外部压制。 */
    public GhostSuppressionState merge(GhostSuppressionState other) {
        return new GhostSuppressionState(external.ordinal() >= other.external.ordinal() ? external : other.external,
                internal.ordinal() >= other.internal.ordinal() ? internal : other.internal, Math.max(pressure, other.pressure));
    }
    /** 自主行动受任何来源的基础及以上压制限制。 */
    public boolean canAct() { return external == GhostSuppression.Level.NONE && internal == GhostSuppression.Level.NONE; }
    /** 自动规则在高度压制及以上停止。 */
    public boolean canRunRules() { return external.ordinal() < 2 && internal.ordinal() < 2; }
    /** 只有属于驾驭者的主动渠道才能豁免体内压制，外来完全压制始终阻止能力。 */
    public boolean canUseAbility(boolean controlled) {
        return external != GhostSuppression.Level.COMPLETE
                && (controlled || internal != GhostSuppression.Level.COMPLETE);
    }
}
