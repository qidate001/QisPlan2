package com.qidate.qisplan2.entity.ghostmist;

import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.entity.GhostAttributeSystem;
import com.qidate.qisplan2.entity.ai.GhostWanderGoal;
import com.qidate.qisplan2.ghost.domain.type.mist.GhostMistDomainController;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * ========================================
 * 鬼雾源头
 * ========================================
 *
 * 鬼雾本体。
 *
 * 鬼域本身属于运行时状态，
 * 而鬼雾的成长数据由实体负责持久化。
 */
public class GhostMist
        extends AbstractGhostEntity {

    private static final double BASE_SUPERNATURAL_STRENGTH = 5.0D;
    private static final double BASE_SUPERNATURAL_DEFENSE = 4.0D;

    /**
     * 鬼雾初始鬼域强度。
     */
    private static final double DEFAULT_DOMAIN_STRENGTH = 5.0D;

    /**
     * 鬼雾初始鬼域半径。
     */
    private static final double DEFAULT_DOMAIN_RADIUS = 80.0D;

    /**
     * ========================================
     * 持久化状态
     * ========================================
     *
     * 这些数据属于“鬼雾自身的成长状态”。
     *
     * GhostDomain 是运行时对象，
     * 而这里的数据会随实体一起保存进世界。
     */
    private double domainStrength =
            DEFAULT_DOMAIN_STRENGTH;

    private double domainRadius =
            DEFAULT_DOMAIN_RADIUS;

    /**
     * NBT Key
     */
    private static final String TAG_DOMAIN_STRENGTH =
            "GhostMistDomainStrength";

    private static final String TAG_DOMAIN_RADIUS =
            "GhostMistDomainRadius";

    public GhostMist(
            EntityType<? extends GhostMist> entityType,
            Level level
    ) {
        super(entityType, level);

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
    public void tick() {
        super.tick();

        if (
                this.level() instanceof ServerLevel level
        ) {
            GhostMistDomainController.createDomain(
                    level,
                    this
            );
        }
    }

    @Override
    protected void registerGoals() {
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
        return ResourceLocation.fromNamespaceAndPath(
                "qisplan2",
                "ghost_mist"
        );
    }

    /**
     * ========================================
     * 鬼域持久化数据
     * ========================================
     */

    public double getDomainStrength() {
        return this.domainStrength;
    }

    public double getDomainRadius() {
        return this.domainRadius;
    }

    public void setDomainStrength(
            double strength
    ) {
        this.domainStrength = strength;
    }

    public void setDomainRadius(
            double radius
    ) {
        this.domainRadius = radius;
    }

    /**
     * ========================================
     * 保存实体数据
     * ========================================
     */
    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(tag);

        tag.putDouble(
                TAG_DOMAIN_STRENGTH,
                this.domainStrength
        );

        tag.putDouble(
                TAG_DOMAIN_RADIUS,
                this.domainRadius
        );
    }

    /**
     * ========================================
     * 读取实体数据
     * ========================================
     */
    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(tag);

        if (tag.contains(TAG_DOMAIN_STRENGTH)) {
            this.domainStrength =
                    tag.getDouble(
                            TAG_DOMAIN_STRENGTH
                    );
        }

        if (tag.contains(TAG_DOMAIN_RADIUS)) {
            this.domainRadius =
                    tag.getDouble(
                            TAG_DOMAIN_RADIUS
                    );
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }
}