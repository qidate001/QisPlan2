package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class InvisibleGhost extends Monster {

    public InvisibleGhost(
            EntityType<? extends Monster> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );
    }

    @Override
    protected void registerGoals() {
        /*
         * 暂时不注册 AI。
         *
         * 它的攻击规律由
         * InvisibleGhostHandler 统一处理。
         */
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(
            net.minecraft.world.damagesource.DamageSource damageSource
    ) {
        /*
         * 先做成灵异实体，不允许普通伤害破坏。
         */
        return true;
    }
}