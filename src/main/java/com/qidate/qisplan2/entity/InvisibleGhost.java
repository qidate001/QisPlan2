package com.qidate.qisplan2.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class InvisibleGhost
        extends AbstractGhostEntity {

    public InvisibleGhost(
            EntityType<? extends InvisibleGhost> entityType,
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
         * 暂时不需要自己的移动 AI。
         *
         * “看到玩家攻击”由
         * InvisibleGhostHandler 统一处理。
         */
    }

    @Override
    public double getSupernaturalDefense() {
        return 0.0D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(
                        Attributes.MAX_HEALTH,
                        20.0D
                )
                .add(
                        Attributes.MOVEMENT_SPEED,
                        0.25D
                )
                .add(
                        Attributes.FOLLOW_RANGE,
                        32.0D
                );
    }
}