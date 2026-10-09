
package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/**
 * 触碰语义事件。
 *
 * 只描述触碰行为本身，不规定触碰产生的具体效果。
 * 模块可以根据自身规则决定如何响应。
 */
public record TouchEvent(
        LivingEntity target
) implements GhostEvent {

    public TouchEvent {
        Objects.requireNonNull(target, "target");
    }
}