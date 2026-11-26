package com.qidate.qisplan2.ghost.module.component;

import com.qidate.qisplan2.ghost.module.suppression.GhostSuppression;

/** 可由鬼域或诅咒选择的常态压制声明；零倍率关闭，不代表无解压制。 */
public record GhostSuppressionAura(int minimumGrade,double multiplier) {
    public static final GhostSuppressionAura NONE=new GhostSuppressionAura(1,0);
    /** 拒绝不合法的声明，避免无限数值进入关系账本。 */
    public GhostSuppressionAura {if(minimumGrade<1||!Double.isFinite(multiplier)||multiplier<0)throw new IllegalArgumentException("Invalid suppression aura");}
    /** 等级媒介成立时按当前有效强度建立压制，交界处削弱也在这里生效。 */
    public GhostSuppression offer(int grade,double effectiveStrength){return grade<minimumGrade||multiplier==0||effectiveStrength<=0?null:new GhostSuppression(GhostSuppression.Kind.NORMAL,effectiveStrength*multiplier);}
}
