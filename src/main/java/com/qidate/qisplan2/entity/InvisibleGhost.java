package com.qidate.qisplan2.entity;

import net.minecraft.world.entity.EntityType;
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
         * 暂时没有特殊 AI。
         */
    }

    @Override
    public double getSupernaturalDefense() {
        return 0.0D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createLivingAttributes()
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