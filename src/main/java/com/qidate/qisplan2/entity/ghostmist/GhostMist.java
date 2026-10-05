package com.qidate.qisplan2.entity.ghostmist;

import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.entity.GhostAttributeSystem;
import com.qidate.qisplan2.entity.ai.GhostWanderGoal;
import com.qidate.qisplan2.ghost.domain.type.mist.GhostMistDomainController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * ========================================
 * 鬼雾
 * ========================================
 *
 * 鬼雾的源头厉鬼实体。
 */
public class GhostMist
        extends AbstractGhostEntity {

    /**
     * ========================================
     * 基础灵异属性
     * ========================================
     */

    /**
     * 初始灵异强度。
     */
    private static final double BASE_SUPERNATURAL_STRENGTH =
            5.0D;

    /**
     * 初始灵异防御。
     */
    private static final double BASE_SUPERNATURAL_DEFENSE =
            4.0D;

    public GhostMist(
            EntityType<? extends GhostMist> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );

        /*
         * ========================================
         * 灵异属性
         * ========================================
         */

        GhostAttributeSystem.setSupernaturalStrength(
                this,
                BASE_SUPERNATURAL_STRENGTH
        );

        GhostAttributeSystem.setSupernaturalDefense(
                this,
                BASE_SUPERNATURAL_DEFENSE
        );
    }

    /**
     * ========================================
     * TICK
     * ========================================
     */
    @Override
    public void tick() {
        super.tick();

        GhostMistDomainController.tick(this);
    }

    /**
     * ========================================
     * AI
     * ========================================
     */
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

    /**
     * ========================================
     * 厉鬼 ID
     * ========================================
     */
    @Override
    public ResourceLocation getGhostId() {
        return ResourceLocation.fromNamespaceAndPath(
                "qisplan2",
                "ghost_mist"
        );
    }

    /**
     * ========================================
     * Minecraft 原版实体属性
     * ========================================
     */
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