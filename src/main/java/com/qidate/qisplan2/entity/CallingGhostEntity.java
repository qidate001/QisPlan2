package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.ghost.bearing.HostComposition;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.implementation.calling.CallingGhostModule;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import java.util.List;
import java.util.Map;

/** 喊人鬼的独特外观宿主。除初始模块外没有专属规则、能力或 AI。 */
public final class CallingGhostEntity extends AnimatedModularGhostEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ghost.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.ghost.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.ghost.attack");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.ghost.hurt");
    private static final Map<String, RawAnimation> VISUAL_ACTIONS = Map.of(
            "attack", ATTACK,
            "call", RawAnimation.begin().thenPlay("animation.ghost.call"));

    /** 新实体自带喊人鬼模块；之后可自由增删，读档完全沿用父类的模块数据。 */
    public CallingGhostEntity(EntityType<? extends CallingGhostEntity> type,Level level) {
        super(type, level, HostComposition.HUMANOID,
                List.of(new GhostModuleData.Entry(
                        CallingGhostModule.ID,
                        CallingGhostModule.INITIAL_INTENSITY,
                        0
                ))
        );
    }

    /**
     * 声明此独特实体的基础属性，由实体属性注册事件显式调用。
     * 在通用宿主属性上覆盖数值，保留框架所需属性；可在这里独立调整
     * 移动速度、最大生命值、追踪范围，或添加护甲等原版属性。
     * 这些数值属于实体宿主，不代表模块的灵异强度；模块的临时效果仍通过修饰符叠加。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return ModularGhostEntity.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** 仅声明此实体外观真实具备的动作；搭载其他模块不会自动获得其动画。 */
    @Override
    protected Map<String, RawAnimation> getVisualActions() {
        return VISUAL_ACTIONS;
    }

    /** 注册本外观的移动与动作控制器；一次性动作独立播放，避免被行走切换截断。 */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<CallingGhostEntity> actionController = new AnimationController<>(
                this, "action", 2, state -> {
                    if (hurtTime > 0) return state.setAndContinue(HURT);
                    if (swinging) return state.setAndContinue(ATTACK);
                    return PlayState.STOP;
                });
        VISUAL_ACTIONS.forEach(actionController::triggerableAnim);
        controllers.add(new AnimationController<>(this, "movement", 4,
                state -> state.setAndContinue(state.isMoving() ? MOVE : IDLE)), actionController);
    }
}
