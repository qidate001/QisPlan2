package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/**
 * 灵异使用事件。
 *
 * 表示某个实体主动使用了一个承载灵异模块的宿主。
 *
 * 事件只描述使用行为，不规定使用行为产生的效果，
 * 也不限制使用者手持的物品。
 */
public record GhostUseEvent(
        LivingEntity user
) implements GhostEvent {

    public GhostUseEvent {
        Objects.requireNonNull(user, "user");
    }
}