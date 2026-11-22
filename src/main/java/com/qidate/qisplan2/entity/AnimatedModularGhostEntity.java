package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.ghost.bearing.HostComposition;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.List;
import java.util.Map;

/**
 * 有独立模型的模块宿主公共基类。只增加动画表现，不增加任何厉鬼规则或能力。
 * 实体子类声明默认模块、身体组成和自身支持的动画；移除模块后仍保留外观，空模块存档也不会被补回。
 * 公共基类不规定动作集合或动画资源名；控制器与动作映射均由具体外观实体注册。
 */
public abstract class AnimatedModularGhostEntity extends ModularGhostEntity implements GeoEntity {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    /** 复用完整模块存储、承载和行为框架，构造阶段不复制任何厉鬼逻辑。 */
    protected AnimatedModularGhostEntity(EntityType<? extends AnimatedModularGhostEntity> type,Level level,
                                         HostComposition composition,List<GhostModuleData.Entry> defaults) {
        super(type,level,composition,defaults);
    }

    /**
     * 声明此种外观支持的动作及其动画。子类使用同一映射注册 action 控制器，
     * 避免播放入口与控制器声明不一致；不支持任何一次性动作时可保留空映射。
     */
    protected Map<String, RawAnimation> getVisualActions() {
        return Map.of();
    }

    /**
     * 接收世界适配器的表现请求，仅同步该外观明确支持的动作。
     * 不支持的动作直接忽略，既不播放替代动画，也不影响模块能力本身的执行。
     */
    public void playVisualAction(String action) {
        if (!level().isClientSide && getVisualActions().containsKey(action)) {
            triggerAnim("action", action);
        }
    }

    /** 每个实体独立缓存动画状态，不让同种实体共享播放进度。 */
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }
}
