package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.death.SupernaturalCombatHandler;
import com.qidate.qisplan2.entity.ai.GhostWanderGoal;
import net.minecraft.world.damagesource.DamageSource;
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

    // 灵异防御
    private static final double SUPERNATURAL_DEFENSE = 4.0D;

    @Override
    public double getSupernaturalDefense() {
        return SUPERNATURAL_DEFENSE;
    }

    /**
     * 不可视之鬼无敌。
     */
    @Override
    public boolean isInvulnerableTo(
            DamageSource damageSource
    ) {
        return SupernaturalCombatHandler.isInvulnerableTo(
                this,
                damageSource
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