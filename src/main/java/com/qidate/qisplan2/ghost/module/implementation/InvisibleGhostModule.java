package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.GhostAbility;
import com.qidate.qisplan2.ghost.module.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.EntityPerceivedEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

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
                0.0D,
                0.0D
        );
    }

    @Override
    public List<GhostRule> getRules() {
        return RULES;
    }

    @Override
    public List<GhostAbility> getAbilities() {
        return ABILITIES;
    }

    private static final class PerceptionRule
            implements GhostRule {

        @Override
        public ResourceLocation getId() {
            return PERCEPTION_RULE_ID;
        }

        @Override
        public boolean supports(GhostEvent event) {
            return event instanceof EntityPerceivedEvent perceivedEvent
                    && perceivedEvent.observer() instanceof Player;
        }

        @Override
        public boolean canTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            return supports(event);
        }

        @Override
        public void onTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            context.tryExecuteAbility(
                    ATTACK_ABILITY_ID,
                    event
            );
        }
    }

    private static final class AttackAbility
            implements GhostAbility {

        @Override
        public ResourceLocation getId() {
            return ATTACK_ABILITY_ID;
        }

        @Override
        public boolean canExecute(GhostAbilityContext context) {
            return context.event() instanceof EntityPerceivedEvent event
                    && event.observer() instanceof Player;
        }

        @Override
        public void execute(GhostAbilityContext context) {
            if (!context.isModuleAvailable()) {
                return;
            }

            if (!(context.event() instanceof EntityPerceivedEvent event)) {
                return;
            }

            if (!(event.observer() instanceof Player player)) {
                return;
            }

            SupernaturalDeathHandler.tryKill(
                    player,
                    ModDamageTypes.invisibleGhost(event.target()),
                    context.getModuleContext().intensity()
            );
        }
    }
}
