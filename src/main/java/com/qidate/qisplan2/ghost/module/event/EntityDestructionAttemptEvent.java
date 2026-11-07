package com.qidate.qisplan2.ghost.module.event;

import java.util.Objects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 灵异语义事件：实体即将被杀死。
 * 这是死亡发生前的事实通知，不是能力请求；模块成功阻止后，由事件桥取消死亡。
 */
public final class EntityDestructionAttemptEvent extends DestructionAttemptEvent {
    private final LivingEntity entity;
    private final DamageSource source;

    public EntityDestructionAttemptEvent(LivingEntity entity, DamageSource source) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.source = Objects.requireNonNull(source, "source");
    }

    public LivingEntity getEntity() { return entity; }
    public DamageSource getSource() { return source; }

    /** 投射物追溯到射手；环境伤害没有可反击的生物攻击者。 */
    @Nullable
    public LivingEntity getAttacker() {
        return source.getEntity() instanceof LivingEntity attacker && attacker != entity
                ? attacker : null;
    }
}
