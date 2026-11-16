package com.qidate.qisplan2.ghost.module.ability;

import net.minecraft.world.entity.LivingEntity;

/** 主动调用的作用对象；由可信服务端提供，模块不能通过它读取自己的宿主。 */
public record GhostInvocationParameters(LivingEntity recipient, Integer number) implements GhostAbilityParameters {
    /** 无输入的普通能力保持原有调用方式。 */
    public GhostInvocationParameters(LivingEntity recipient) { this(recipient, null); }
}
