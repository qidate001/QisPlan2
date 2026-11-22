package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.ghost.bearing.HostComposition;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.List;

/**
 * 有独立模型的模块宿主公共基类。只增加动画表现，不增加任何厉鬼规则或能力。
 * 实体子类仅声明默认模块和身体组成；移除模块后仍保留外观，空模块存档也不会被补回。
 * 各实体动画资源沿用 animation.ghost.* 通用动作名，由资源文件定义实际姿势。
 */
public abstract class AnimatedModularGhostEntity extends ModularGhostEntity implements GeoEntity {
    private final AnimatableInstanceCache animationCache=GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ghost.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.ghost.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.ghost.attack");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.ghost.hurt");
    private static final RawAnimation CALL = RawAnimation.begin().thenPlay("animation.ghost.call");

    /** 复用完整模块存储、承载和行为框架，构造阶段不复制任何厉鬼逻辑。 */
    protected AnimatedModularGhostEntity(EntityType<? extends AnimatedModularGhostEntity> type,Level level,
                                         HostComposition composition,List<GhostModuleData.Entry> defaults) {
        super(type,level,composition,defaults);
    }

    /** 基础姿势平滑切换；一次性动作与移动分层，避免移动截断喊名动作。 */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"movement",4,
                state -> state.setAndContinue(state.isMoving()?MOVE:IDLE)),
                new AnimationController<>(this,"action",2,state -> {
                    if(hurtTime > 0) return state.setAndContinue(HURT);
                    if(swinging) return state.setAndContinue(ATTACK);
                    return PlayState.STOP;
                }).triggerableAnim("call", CALL)
                        .triggerableAnim("attack", ATTACK)
                        .triggerableAnim("knock", RawAnimation.begin()
                                .thenPlay("animation.ghost.knock")));
    }

    /** 世界适配器报告动作表现；服务端通过 GeckoLib 同步，不以动画驱动能力。 */
    public void playVisualAction(String action) {
        if(!level().isClientSide) triggerAnim("action",action);
    }

    /** 每个实体独立缓存动画状态，不让同种实体共享播放进度。 */
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
}
