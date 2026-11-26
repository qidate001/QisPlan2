package com.qidate.qisplan2.ghost.module.implementation;

import java.util.Set;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.domain.shape.CylinderDomainShape;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainDefinition;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainShaderEffect;
import com.qidate.qisplan2.ghost.module.implementation.umbrella.GhostRainEffect;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.ArrayList;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainPanel;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainPresets;
import java.util.Optional;

/** 鬼雨伞的完整定义：撑开鬼域，鬼雨袭击与反噬均属于模块能力。 */
public final class GhostUmbrellaModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.parse("qisplan2:ghost_umbrella");
    public static final ResourceLocation TOGGLE = ResourceLocation.parse("qisplan2:ghost_umbrella/domain");
    private static final ResourceLocation RAIN = ResourceLocation.parse("qisplan2:ghost_umbrella/rain");
    private static final ResourceLocation STILL = ResourceLocation.parse("qisplan2:ghost_umbrella/still");

    /** 模块身份也是鬼域类型和渲染资源身份。 */
    @Override public ResourceLocation getId(){return ID;}

    /** 沿用原有鬼域强度；袭击倍率由能力单独定义。 */
    @Override public GhostModuleInitialData getInitialData(){return new GhostModuleInitialData(100,0);}

    /** 雨幕使用圆柱几何，三层起始；不开启框架透视和化虹。 */
    @Override public Optional<GhostDomainDefinition> domainDefinition(){
        return Optional.of( new GhostDomainDefinition(
                50,
                3,
                false,
                CylinderDomainShape::new,
                true,false,
                20,
                false,
                false,
                List.of(new GhostDomainShaderEffect(ID, "GhostUmbrella"), GhostRainEffect.INSTANCE))
        );
    }

    /** 所有能力都要求真实暴露，雨幕袭击额外验证本模块鬼域的有效覆盖。 */
    @Override public List<GhostMedium> getMedia(){return List.of(new GhostMedium(){
        /** 返回当前媒介、规则或能力的稳定标识。 */
        public ResourceLocation getId(){return ResourceLocation.parse("qisplan2:ghost_umbrella/exposed");}
        /** 校验真实暴露与有效覆盖，不从宿主类型推断媒介。 */
        public boolean allows(GhostAbilityContext c){
            var world=c.getModuleContext().world();
            if(!world.present())return false;
            return !c.abilityId().equals(RAIN) || c.event() instanceof GhostDomainExposureEvent e && world.containsInDomain(e.domainId(),e.target());
        }
    });}

    /** 使用事实只请求开关；死机物品规则不触发，但驾驭主动调用仍可使用能力。 */
    @Override public List<GhostRule> getRules(){return List.of(new GhostRule(){
        /** 返回当前媒介、规则或能力的稳定标识。 */
        public ResourceLocation getId(){return ResourceLocation.parse("qisplan2:ghost_umbrella/use");}
        /** 只接受已经发生的使用事实。 */
        public Set<Class<? extends GhostEvent>> eventTypes() { return Set.of(GhostUseEvent.class); }

        /** 规则只在当前环境具备鬼域出口时触发。 */
        public boolean canTrigger(GhostRuleContext c,GhostEvent e){return c.getModuleContext().world().canToggleDomain();}
        /** 通过统一执行器请求开关能力。 */
        public void onTrigger(GhostRuleContext c,GhostEvent e){c.getModuleContext().reactAbility(TOGGLE,e);}
    });}

    /** 展开的鬼域维持停步代价；能力状态由统一执行器校验。 */
    @Override public boolean ticksAbilities() { return true; }

    /** 维护已启动能力，由注册时声明的 Tick 订阅调度。 */
    public void tickAbilities(GhostModuleContext c){if(c.world().domain().exists())c.continueAbility(STILL,null);}

    /** 周期曝光继续已展开的雨幕能力，不让域控制器直接实施袭击。 */
    @Override public Set<Class<? extends GhostEvent>> abilityEventTypes() { return Set.of(GhostDomainExposureEvent.class); }

    /** 仅接收已声明订阅的持续能力事实。 */
    public void onAbilityEvent(GhostModuleContext c,GhostEvent event){
        if(event instanceof GhostDomainExposureEvent e && c.world().containsInDomain(e.domainId(),e.target()))c.continueAbility(RAIN,e);
    }

    /** 开关公开，雨幕和停步是内部持续能力。 */
    @Override public List<GhostAbility> getIntrinsicAbilities(){List<GhostAbility> abilities = new ArrayList<>(List.of(new GhostAbility(){
        /** 返回当前媒介、规则或能力的稳定标识。 */
        public ResourceLocation getId(){return TOGGLE;}
        /** 公开技能使用现有鬼伞纹理。 */
        public ResourceLocation iconTexture(){return ResourceLocation.parse("qisplan2:textures/item/ghost_umbrella_open.png");}
        /** 检查该能力的环境条件与持续时间限制。 */
        public boolean canExecute(GhostAbilityContext c){
            var m=c.getModuleContext();
            return m.world().canToggleDomain() && (!m.world().domain().exists() || m.world().gameTime()-m.getCustomData().getLong("domainOpenedAt")>=200);
        }
        /** 执行已通过状态和媒介检查的能力。 */
        public void execute(GhostAbilityContext c){executeWithResult(c);}
        /** 返回实际开关结果，并记录展开时间。 */
        public boolean executeWithResult(GhostAbilityContext c){
            var m=c.getModuleContext();
            if(!m.world().toggleDomain())return false;
            if(m.world().domain().exists()){var data=m.getCustomData();data.putLong("domainOpenedAt",m.world().gameTime());m.setCustomData(data);}
            return true;
        }
    }, new GhostAbility(){
        /** 返回当前媒介、规则或能力的稳定标识。 */
        public ResourceLocation getId(){return RAIN;}
        /** 内部持续效果不进入技能轮盘。 */
        public boolean isPublicSkill(){return false;}
        /** 检查该能力的环境条件与持续时间限制。 */
        public boolean canExecute(GhostAbilityContext c){return c.event() instanceof GhostDomainExposureEvent;}
        /** 自身反噬为鬼雨强度四分之一；仍通过统一影响裁决。 */
        public GhostAbilityInfluence activeInfluence(GhostAbilityContext c){
            var e=(GhostDomainExposureEvent)c.event();
            return GhostAbilityInfluence.of(e.target(),GhostInfluenceType.ATTACK,strength(c,e));
        }
        /** 根据模块强度计算雨幕袭击及四分之一的自身反噬。 */
        private double strength(GhostAbilityContext c,GhostDomainExposureEvent e){return c.getModuleContext().intensity()/10*(c.getModuleContext().world().isOrigin(e.target())?.25:1);}
        /** 执行已通过状态和媒介检查的能力。 */
        public void execute(GhostAbilityContext c){var e=(GhostDomainExposureEvent)c.event();c.getModuleContext().world().attack(e.target(),ModDamageTypes.GHOST_UMBRELLA,strength(c,e));}
    }, new GhostAbility(){
        /** 返回当前媒介、规则或能力的稳定标识。 */
        public ResourceLocation getId(){return STILL;}
        /** 内部持续效果不进入技能轮盘。 */
        public boolean isPublicSkill(){return false;}
        /** 检查该能力的环境条件与持续时间限制。 */
        public boolean canExecute(GhostAbilityContext c){return c.getModuleContext().world().domain().exists();}
        /** 执行已通过状态和媒介检查的能力。 */
        public void execute(GhostAbilityContext c){c.getModuleContext().world().immobilizeSource();}
    }));
        abilities.addAll(GhostDomainPresets.commonAbilities(ID));
        return List.copyOf(abilities);
    }

    /** 鬼域页面从本模块声明生成，不要求界面认识具体厉鬼。 */
    @Override public GhostDomainPanel domainPanel() { return new GhostDomainPanel(List.of(),List.of(),List.of(),GhostDomainPresets.common(ID,TOGGLE)); }
}
