package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.GhostAbility;
import com.qidate.qisplan2.ghost.module.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.GhostMedium;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

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
        return new GhostModuleInitialData(20.0D, 0.0D, 0.0D);
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
    public List<GhostAbility> getAbilities() {
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
            return context.event() instanceof TouchEvent;
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
            return event instanceof TouchEvent;
        }

        @Override
        public boolean canTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            return event instanceof TouchEvent;
        }

        @Override
        public void onTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            if (!(event instanceof TouchEvent)) {
                return;
            }

            context.tryExecuteAbility(
                    ATTACK_ABILITY_ID,
                    event
            );
        }
    }

    /**
     * 攻击能力：复苏值增加，并对触碰目标施加灵异袭击。
     */
    private static final class UntouchableAttackAbility
            implements GhostAbility {

        @Override
        public ResourceLocation getId() {
            return ATTACK_ABILITY_ID;
        }

        @Override
        public boolean canExecute(GhostAbilityContext context) {
            return context.event() instanceof TouchEvent;
        }

        @Override
        public void execute(GhostAbilityContext context) {
            if (!context.isModuleAvailable()) {
                return;
            }

            if (!(context.event() instanceof TouchEvent touchEvent)) {
                return;
            }

            GhostModuleContext moduleContext =
                    context.getModuleContext();

            moduleContext.setRevival(
                    moduleContext.revival() + 1.0D
            );

            SupernaturalDeathHandler.tryKill(
                    touchEvent.target(),
                    ModDamageTypes.untouchableGhost(
                            touchEvent.source()
                    ),
                    moduleContext.intensity()
            );
        }
    }
}