package com.qidate.qisplan2.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
 * - 预览厉鬼待机、移动、攻击、受击和歪头动画
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

    private static final RawAnimation IDLE = RawAnimation.begin()
            .thenLoop("animation.qisplan2_corpse_ghost.idle");
    private static final RawAnimation MOVE = RawAnimation.begin()
            .thenLoop("animation.qisplan2_corpse_ghost.move");
    private static final RawAnimation ATTACK = RawAnimation.begin()
            .thenPlay("animation.qisplan2_corpse_ghost.attack");
    private static final RawAnimation HURT = RawAnimation.begin()
            .thenPlay("animation.qisplan2_corpse_ghost.hurt");
    private static final RawAnimation LOOK = RawAnimation.begin()
            .thenPlay("animation.qisplan2_corpse_ghost.look");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "movement", 4,
                        state -> state.setAndContinue(state.isMoving() ? MOVE : IDLE)),
                new AnimationController<>(this, "action", 0, state -> {
                    if (this.hurtTime > 0) {
                        return state.setAndContinue(HURT);
                    }
                    return PlayState.STOP;
                })
                        .triggerableAnim("attack", ATTACK)
                        .triggerableAnim("look", LOOK)
        );
    }

    /** Right-click previews attack; sneak-right-click previews the head tilt. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            triggerAnim("action", player.isShiftKeyDown() ? "look" : "attack");
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}