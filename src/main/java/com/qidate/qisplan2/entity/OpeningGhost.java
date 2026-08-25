package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.death.ModDamageTypes;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class OpeningGhost
        extends AbstractDoorTriggerGhost {

    private static final double SUPERNATURAL_DEFENSE =
            6.0D;

    public OpeningGhost(
            EntityType<? extends OpeningGhost> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );
    }

    @Override
    public double getSupernaturalDefense() {
        return SUPERNATURAL_DEFENSE;
    }

    @Override
    protected DamageSource getAttackDamageSource(
            Entity source
    ) {
        return ModDamageTypes.openingGhost(
                this
        );
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