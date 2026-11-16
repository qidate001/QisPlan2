package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;

/** 观察到某实体携带食物这一事实，不代表必须袭击；执行时仍须重新检查食物和位置。 */
public record FoodCarriedEvent(LivingEntity target) implements GhostEvent {
    /** 语义事实必须具有目标。 */
    public FoodCarriedEvent { Objects.requireNonNull(target); }
}
