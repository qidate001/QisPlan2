package com.qidate.qisplan2.entity.invisible;

import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.entity.GhostAttributeSystem;
import com.qidate.qisplan2.entity.ai.GhostWanderGoal;
import com.qidate.qisplan2.ghost.possession.ability.knockingghost.KnockingGhostAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class InvisibleGhost
        extends AbstractGhostEntity {

    /**
     * 初始灵异强度。
     */
    private static final double BASE_SUPERNATURAL_STRENGTH = 5.0D;

    /**
     * 初始灵异防御。
     */
    private static final double BASE_SUPERNATURAL_DEFENSE = 4.0D;

    public InvisibleGhost(
            EntityType<? extends InvisibleGhost> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );

        GhostAttributeSystem.setSupernaturalStrength(
                this,
                BASE_SUPERNATURAL_STRENGTH
        );

        GhostAttributeSystem.setSupernaturalDefense(
                this,
                BASE_SUPERNATURAL_DEFENSE
        );
    }

    @Override
    protected void registerGoals() {
        /*
         * ========================================
         * 四处游荡
         * ========================================
         */
        this.goalSelector.addGoal(
                8,
                new GhostWanderGoal(
                        this,
                        0.7D
                )
        );
    }

    @Override
    public ResourceLocation getGhostId() {
        return KnockingGhostAbility.ID;
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