package com.qidate.qisplan2.ghost.domain;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;

public final class GhostDomainEntityPolicy {

    private GhostDomainEntityPolicy() {
    }

    /**
     * 判断实体是否应该参与鬼域实体追踪。
     *
     * <p>
     * 默认允许实体进入追踪系统，
     * 仅排除目前明确没有必要参与鬼域关系计算的实体。
     */
    public static boolean shouldTrack(
            Entity entity
    ) {

        return !(entity instanceof ItemEntity)
                && !(entity instanceof ExperienceOrb)
                && !(entity instanceof Painting)
                && !(entity instanceof ItemFrame)
                && !(entity instanceof GlowItemFrame);
    }
}