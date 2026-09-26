package com.qidate.qisplan2.entity.ghostdoor.opening;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.entity.GhostAttributeSystem;
import com.qidate.qisplan2.entity.ghostdoor.AbstractDoorTriggerGhost;
import com.qidate.qisplan2.ghost.possession.ability.ghostdoor.opening.OpeningGhostAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class OpeningGhost
        extends AbstractDoorTriggerGhost {

    public OpeningGhost(
            EntityType<? extends OpeningGhost> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );

        GhostAttributeSystem.setSupernaturalDefense(
                this,
                6.0D
        );
    }

    @Override
    public ResourceLocation getGhostId() {
        return OpeningGhostAbility.ID;
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