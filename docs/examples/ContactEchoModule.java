package com.qidate.qisplan2.ghost.module.implementation.example;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.GhostMedium;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityStimulation;
import com.qidate.qisplan2.ghost.module.ability.GhostTargetParameters;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceCollector;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 教学模块：触响鬼。
 * 规则：开始接触时尝试袭击；媒介：存在有效接触目标；能力：接触袭击。
 * 定义不持有任何宿主对象或单个实例的可变状态。
 */
public final class ContactEchoModule implements GhostModule {
    public static final ResourceLocation ID = id("contact_echo");
    public static final ResourceLocation ATTACK = id("contact_echo/contact_attack");
    private static final ResourceLocation RULE = id("contact_echo/contact_begin");
    private static final ResourceLocation MEDIUM = id("contact_echo/contact");
    private static final ResourceLocation ICON =
            ResourceLocation.parse("minecraft:textures/item/iron_sword.png");
    private static final List<GhostAbility> ABILITIES = List.of(new ContactAttack());
    private static final List<GhostRule> RULES = List.of(new ContactRule());

    /** 所有定义标识使用模块所在命名空间。 */
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("qisplan2", path);
    }

    /** 返回整只厉鬼的唯一身份。 */
    @Override public ResourceLocation getId() { return ID; }

    /** 只影响新建实例，不覆盖存档中的实时属性。 */
    @Override public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(10, 0);
    }

    /** 教学示例不自动增长复苏；能力成功时单独结算刺激。 */
    @Override public double getNaturalRevivalRate() { return 0; }

    /** 模块图标与技能图标分别声明，即使本例复用同一纹理。 */
    @Override public ResourceLocation iconTexture() { return ICON; }

    /** 规则只是自动调用入口。 */
    @Override public List<GhostRule> getRules() { return RULES; }

    /** 唯一技能列表；自动规则和轮盘均使用这里的同一能力。 */
    @Override public List<GhostAbility> getIntrinsicAbilities() { return ABILITIES; }

    /** 本例只有一个技能，接触条件作为模块共用媒介。 */
    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            /** 媒介 ID 与规则、能力 ID 分开命名。 */
            @Override public ResourceLocation getId() { return MEDIUM; }

            /** 仅检查条件，不支付代价、不改变世界。 */
            @Override public boolean allows(GhostAbilityContext context) {
                return contactTarget(context) != null;
            }
        });
    }

    /**
     * 自动调用使用可信接触事实；直接调用使用输入目标或世界视线。
     * 主动调用不能凭空制造接触，必须由世界接口再次验证实际接触关系。
     * 静态来源的 TouchEvent.source 可以为空，因此不把它当作必须存在的实体。
     */
    private static LivingEntity contactTarget(GhostAbilityContext context) {
        if (context.event() instanceof TouchEvent event) {
            LivingEntity target = event.target();
            return target.isAlive() && !target.level().isClientSide() ? target : null;
        }
        var world = context.getModuleContext().world();
        LivingEntity target = context.parameters() instanceof GhostTargetParameters input
                ? input.target() : world.aimedTarget(3);
        return target != null && target.isAlive() && !target.level().isClientSide()
                && world.touching(target) ? target : null;
    }

    /** 开始接触是自动触发条件；持续接触不重复触发本规则。 */
    private static final class ContactRule implements GhostRule {
        /** 返回规则标识。 */
        @Override public ResourceLocation getId() { return RULE; }

        /** 只订阅真正的接触语义事实。 */
        @Override public boolean supports(GhostEvent event) {
            return event instanceof TouchEvent;
        }

        /** 自动规则要求接触刚发生；这一条件不限制主动技能。 */
        @Override public boolean canTrigger(GhostRuleContext context, GhostEvent event) {
            return event instanceof TouchEvent touch && touch.isBeginning()
                    && touch.target().isAlive() && !touch.target().level().isClientSide();
        }

        /** 收集同目标保护诅咒，再提交袭击影响，裁决获准后才执行能力。 */
        @Override public void onTrigger(GhostRuleContext context, GhostEvent event) {
            var target = ((TouchEvent) event).target();
            var server = target.getServer();
            if (server == null) return;
            GhostInfluenceCollector.collectCurses(context, target.getUUID(), server);
            context.submitAbilityInfluence(ATTACK, event, target.getUUID(),
                    GhostInfluenceType.ATTACK, context.getModuleContext().effectiveIntensity());
        }
    }

    /** 真正的技能实现；这里不认识实体宿主、玩家宿主或物品宿主。 */
    private static final class ContactAttack implements GhostAbility {
        /** 返回技能 ID，语言键默认由它推导。 */
        @Override public ResourceLocation getId() { return ATTACK; }

        /** 返回完整纹理资源路径。 */
        @Override public ResourceLocation iconTexture() { return ICON; }

        /** 重检目标，防止延迟执行时继续使用已失效对象。 */
        @Override public boolean canExecute(GhostAbilityContext context) {
            return contactTarget(context) != null;
        }

        /** 每次实际击杀成功增加一点复苏，由执行器统一结算。 */
        @Override public GhostAbilityStimulation getStimulation(GhostAbilityContext context) {
            return new GhostAbilityStimulation(1,
                    GhostAbilityStimulation.Timing.ON_EFFECT_SUCCESS);
        }

        /** 兼容不需要效果返回值的调用者。 */
        @Override public void execute(GhostAbilityContext context) {
            executeWithResult(context);
        }

        /** 通过抽象世界接口发动袭击，并返回真实结果。 */
        @Override public boolean executeWithResult(GhostAbilityContext context) {
            LivingEntity target = contactTarget(context);
            if (target == null) return false;
            var module = context.getModuleContext();
            // 教学示例复用不可触之鬼伤害类型；正式发布时可注册独立伤害类型与死亡译文。
            return module.world().attack(target, ModDamageTypes.UNTOUCHABLE_GHOST,
                    module.effectiveIntensity());
        }
    }
}
