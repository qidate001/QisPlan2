package com.qidate.qisplan2.ghost.module.environment;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import java.util.List;

/** 模块声明的身体效果快照。是否存在生命、速度或药水系统由宿主投影层决定。 */
public record GhostBodyEffects(double movementSpeed,List<Holder<MobEffect>> effects) {
    public static final GhostBodyEffects NONE=new GhostBodyEffects(0,List.of());
    /** 保存独立只读声明，拒绝非有限速度污染实体属性。 */
    public GhostBodyEffects { if(!Double.isFinite(movementSpeed))throw new IllegalArgumentException("Invalid speed");effects=List.copyOf(effects); }
}
