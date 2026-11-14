package com.qidate.qisplan2.ghost.module.implementation;

import net.minecraft.world.entity.LivingEntity;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityStimulation;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import static com.qidate.qisplan2.QisPlan2.MODID;

public final class UntouchableGhostModule implements GhostModule {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "untouchable_ghost"
            );

    private static final ResourceLocation TOUCH_MEDIUM_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "untouchable_ghost/touch_medium"
            );

    private static final ResourceLocation TOUCH_RULE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "untouchable_ghost/touch_rule"
            );

    private static final ResourceLocation ATTACK_ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "untouchable_ghost/attack"
            );

    private static final GhostMedium TOUCH_MEDIUM =
            new UntouchableTouchMedium();

    private static final GhostAbility ATTACK_ABILITY =
            new UntouchableAttackAbility();

    private static final GhostRule TOUCH_RULE =
            new UntouchableTouchRule();

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                20.0D,
                0.0D
        );
    }

    @Override
    public List<GhostMedium> getMedia() {
        return List.of(TOUCH_MEDIUM);
    }

    @Override
    public List<GhostRule> getRules() {
        return List.of(TOUCH_RULE);
    }

    @Override
    public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(ATTACK_ABILITY);
    }

    /**
     * 不可触之鬼的媒介：必须有触碰事件。
     */
    private static final class UntouchableTouchMedium
            implements GhostMedium {

        @Override
        public ResourceLocation getId() {
            return TOUCH_MEDIUM_ID;
        }

        @Override
        public boolean allows(GhostAbilityContext context) {
            return contactTarget(context) != null;
        }
    }

    /**
     * 触碰规则：触碰事件触发攻击能力。
     */
    private static final class UntouchableTouchRule
            implements GhostRule {

        @Override
        public ResourceLocation getId() {
            return TOUCH_RULE_ID;
        }

        @Override
        public boolean supports(GhostEvent event) {
            return event instanceof TouchEvent touch && touch.isBeginning();
        }

        @Override
        public boolean canTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            return event instanceof TouchEvent touch && touch.isBeginning();
        }

        @Override
        public void onTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            if (!(event instanceof TouchEvent)) {
                return;
            }

            if (!(event instanceof TouchEvent touchEvent)) {
                return;
            }

            context.submitAbilityInfluence(
                    ATTACK_ABILITY_ID,
                    event,
                    touchEvent.target().getUUID(),
                    GhostInfluenceType.ATTACK,
                    context.getModuleContext().intensity()
            );
        }
    }

    /**
     * 攻击能力：复苏值增加，并对触碰目标施加灵异袭击。
     */
    private static final class UntouchableAttackAbility
            implements GhostAbility {
            /** 技能图标由所属模块直接声明。 */
            @Override public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/iron_sword.png"); }


        @Override
        public ResourceLocation getId() {
            return ATTACK_ABILITY_ID;
        }

        @Override
        public GhostAbilityStimulation getStimulation(
                GhostAbilityContext context
        ) {
            return new GhostAbilityStimulation(
                    1.0D,
                    GhostAbilityStimulation.Timing.ON_EFFECT_SUCCESS
            );
        }

        @Override
        public boolean canExecute(GhostAbilityContext context) {
            return contactTarget(context) != null;
        }

        @Override
        public void execute(GhostAbilityContext context) {
            executeWithResult(context);
        }

        @Override
        public boolean executeWithResult(GhostAbilityContext context) {
            if (!context.isModuleAvailable()) {
                return false;
            }

            if (contactTarget(context) == null) return false;

            GhostModuleContext moduleContext =
                    context.getModuleContext();

            return moduleContext.world().attack(contactTarget(context),
                    ModDamageTypes.UNTOUCHABLE_GHOST, moduleContext.intensity());
        }
    }

    /** 接触是媒介，不要求先触发自动规则；主动释放仍必须正在接触目标。 */
    private static LivingEntity contactTarget(GhostAbilityContext context) {
        if (context.event() instanceof TouchEvent touch)
            return touch.target().isAlive() && !touch.target().level().isClientSide() ? touch.target() : null;
        var world = context.getModuleContext().world();
        var target = world.aimedTarget(3);
        return target != null && target.isAlive() && world.touching(target) ? target : null;
    }
}
