package com.qidate.qisplan2.ghost.module.implementation;

import java.util.Set;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityInfluence;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityStimulation;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.SourceSeenEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 不可视之鬼模块。
 *
 * 玩家看见携带该模块的鬼时，
 * 该鬼以模块强度发动灵异袭击。
 */
public final class InvisibleGhostModule implements GhostModule {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "invisible_ghost"
            );

    private static final ResourceLocation PERCEPTION_RULE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "invisible_ghost/perception_rule"
            );

    private static final ResourceLocation ATTACK_ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "invisible_ghost/attack"
            );

    private static final List<GhostRule> RULES =
            List.of(new PerceptionRule());

    private static final List<GhostAbility> ABILITIES =
            List.of(new AttackAbility());

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
    public List<GhostRule> getRules() {
        return RULES;
    }

    @Override
    public List<GhostAbility> getIntrinsicAbilities() {
        return ABILITIES;
    }

    private static final class PerceptionRule
            implements GhostRule {

        @Override
        public ResourceLocation getId() {
            return PERCEPTION_RULE_ID;
        }

        @Override
        public Set<Class<? extends GhostEvent>> eventTypes() { return Set.of(SourceSeenEvent.class); }

        /** 已由订阅索引定位；此处只判断事件携带的事实。 */
        public boolean matches(GhostEvent event) {
            return event instanceof SourceSeenEvent perceivedEvent
                    && perceivedEvent.observer() instanceof Player;
        }

        @Override
        public boolean canTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            return matches(event);
        }

        @Override
        public void onTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            if (!(event instanceof SourceSeenEvent perceivedEvent)
                    || !(perceivedEvent.observer() instanceof Player player)) {
                return;
            }

            context.submitAbilityInfluence(
                    ATTACK_ABILITY_ID,
                    event,
                    player.getUUID(),
                    GhostInfluenceType.ATTACK,
                    context.getModuleContext().intensity()
            );
        }
    }

    private static final class AttackAbility
            implements GhostAbility {
        /** 主动渠道与规则使用相同的袭击/保护语义，获准前不执行世界效果。 */
        @Override public GhostAbilityInfluence activeInfluence(GhostAbilityContext context) {
            return GhostAbilityInfluence.of(context.getModuleContext().world().aimedTarget(8), GhostInfluenceType.ATTACK, context.getModuleContext().intensity());
        }

            /** 技能图标由所属模块直接声明。 */
            @Override public ResourceLocation iconTexture() { return ResourceLocation.parse("minecraft:textures/item/ender_eye.png"); }


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
            return context.event() instanceof SourceSeenEvent event
                    && event.observer() instanceof Player || context.event() == null
                    && context.getModuleContext().world().validTarget(context.getModuleContext().world().aimedTarget(8));
        }

        @Override
        public void execute(GhostAbilityContext context) {
            executeWithResult(context);
        }

        @Override
        public boolean executeWithResult(
                GhostAbilityContext context
        ) {
            if (!context.isModuleAvailable()) {
                return false;
            }

            if (context.event() == null) {
                var module = context.getModuleContext();
                return module.world().attack(module.world().aimedTarget(8), ModDamageTypes.INVISIBLE_GHOST, module.intensity());
            }

            if (!(context.event() instanceof SourceSeenEvent event)) return false;

            if (!(event.observer() instanceof Player player)) return false;

            return context.getModuleContext().world().attack(
                    player, ModDamageTypes.INVISIBLE_GHOST, context.getModuleContext().intensity());
        }
    }
}
