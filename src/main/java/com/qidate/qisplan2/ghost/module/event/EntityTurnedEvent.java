package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;

/**
 * 观察者记录到某个实体水平朝向发生变化的事实。
 * 事件只保存变化前后的角度；多少度算触犯规则、是否袭击由模块判断。
 */
public record EntityTurnedEvent(LivingEntity observer, LivingEntity entity,
        float previousYaw, float currentYaw) implements GhostEvent {
    public EntityTurnedEvent {
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(entity, "entity");
    }
}
