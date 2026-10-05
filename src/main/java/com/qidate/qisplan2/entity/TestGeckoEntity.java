package com.qidate.qisplan2.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * ============================================================
 * QisPlan2 - GeckoLib 测试实体
 * ============================================================
 *
 * 用于验证：
 *
 * 1. GeckoLib 是否能够作为 Jar-in-Jar 正常工作
 * 2. Entity 是否能够实现 GeoEntity
 * 3. Bedrock / Blockbench 模型是否能够被 GeckoLib 加载
 * 4. GeoModel + GeoEntityRenderer 是否能够正常渲染
 *
 * 当前阶段：
 * - 不添加 AI
 * - 不添加动画
 * - 不添加特殊能力
 * - 不添加任何 QisPlan2 灵异逻辑
 *
 * 仅作为 GeckoLib 渲染链路测试实体。
 */
public class TestGeckoEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache geoCache =
            GeckoLibUtil.createInstanceCache(this);

    public TestGeckoEntity(
            EntityType<? extends TestGeckoEntity> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    /**
     * ============================================================
     * 实体基础属性
     * ============================================================
     *
     * PathfinderMob / LivingEntity 在构造阶段就需要属性表，
     * 因此必须为这个测试实体注册 AttributeSupplier。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        controllers.add(
                new AnimationController<>(
                        this,
                        "test_controller",
                        0,
                        state -> {
                            if (state.isMoving()) {
                                return state.setAndContinue(
                                        RawAnimation.begin()
                                                .thenLoop(
                                                        "animation.qisplan2_test_gecko.move"
                                                )
                                );
                            }

                            return PlayState.STOP;
                        }
                )
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}