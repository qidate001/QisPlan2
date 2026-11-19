package com.qidate.qisplan2.ghost.module.suppression;

/** 一条持续压制的声明。无解压制独立建模，不将无穷大写入存档。 */
public record GhostSuppression(Kind kind, double strength) {
    public enum Kind { NORMAL, ABSOLUTE }
    public enum Level { NONE, BASIC, HIGH, COMPLETE }

    /** 普通强度必须有限且非负，无解压制不参与数值运算。 */
    public GhostSuppression {
        if (kind == null || !Double.isFinite(strength) || strength < 0)
            throw new IllegalArgumentException("Invalid suppression");
    }
    /** 创建由媒介维持的无解压制。 */
    public static GhostSuppression absolute() { return new GhostSuppression(Kind.ABSOLUTE, 0); }
    /** 严格大于才进入下一等级；比较目标本质强度，避免压制递归放大。 */
    public Level level(double targetStrength) {
        if (kind == Kind.ABSOLUTE) return Level.COMPLETE;
        if (strength > targetStrength * 1.5) return Level.COMPLETE;
        if (strength > targetStrength * 1.3) return Level.HIGH;
        return strength > targetStrength ? Level.BASIC : Level.NONE;
    }
}
