package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * 灵异使用事件。
 *
 * 表示某个实体主动使用了一个承载灵异模块的宿主。
 *
 * 事件只描述使用行为，不规定使用行为产生的效果，
 * 也不限制使用者手持的物品。
 */
public record GhostUseEvent(
        LivingEntity user,
        @Nullable LivingEntity target
) implements GhostEvent {

    /** 无指定目标的使用事实，如使用物品、操作工作台。 */
    public GhostUseEvent(LivingEntity user) { this(user, null); }

    /** 目标仅描述此次操作的作用对象，不表示模块宿主。 */
    public GhostUseEvent {
        Objects.requireNonNull(user, "user");
    }
}