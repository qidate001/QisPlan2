package com.qidate.qisplan2.ghost.module;

import net.minecraft.resources.ResourceLocation;

/**
 * 厉鬼媒介。
 *
 * 媒介是灵异效果成立的强制性限制。
 * 无论能力由规则触发还是被主动调用，
 * 都必须满足对应媒介的约束。
 *
 * 媒介不受规则沉睡状态影响。
 */
public interface GhostMedium {

    /**
     * 获取媒介 ID。
     */
    ResourceLocation getId();

    /**
     * 判断当前能力调用是否满足媒介条件。
     *
     * 该方法只验证媒介，不负责判断规则状态，
     * 也不负责执行能力。
     */
    boolean allows(GhostAbilityContext context);
}