package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.core.ModTags;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.GhostInscriptionCapability;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.GhostUseEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 鬼墓碑模块。
 *
 * 响应灵异使用事件，并请求进行题字。
 */
public final class GhostTombstoneModule implements GhostModule {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_tombstone"
            );

    private static final ResourceLocation USE_RULE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_tombstone/use_rule"
            );

    private static final ResourceLocation INSCRIPTION_ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_tombstone/inscription"
            );

    private static final List<GhostRule> RULES =
            List.of(new TombstoneUseRule());

    private static final List<GhostAbility> ABILITIES =
            List.of(new InscriptionAbility());

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                25.0D,
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

    private static final class TombstoneUseRule
            implements GhostRule {

        @Override
        public ResourceLocation getId() {
            return USE_RULE_ID;
        }

        @Override
        public boolean supports(GhostEvent event) {
            return event instanceof GhostUseEvent;
        }

        @Override
        public boolean canTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            return event instanceof GhostUseEvent
                    && context.getModuleContext().host()
                    instanceof GhostInscriptionCapability;
        }

        @Override
        public void onTrigger(
                GhostRuleContext context,
                GhostEvent event
        ) {
            context.tryExecuteAbility(
                    INSCRIPTION_ABILITY_ID,
                    event
            );
        }
    }

    private static final class InscriptionAbility
            implements GhostAbility {

        @Override
        public ResourceLocation getId() {
            return INSCRIPTION_ABILITY_ID;
        }

        @Override
        public boolean canExecute(GhostAbilityContext context) {
            return context.event() instanceof GhostUseEvent useEvent
                    && useEvent.user() instanceof Player player
                    && context.host() instanceof GhostInscriptionCapability
                    && isInscribable(player.getMainHandItem());
        }

        @Override
        public void execute(GhostAbilityContext context) {
            if (!context.isModuleAvailable()) {
                return;
            }

            if (!(context.event() instanceof GhostUseEvent useEvent)) {
                return;
            }

            if (!(context.host() instanceof GhostInscriptionCapability capability)) {
                return;
            }

            if (!(useEvent.user() instanceof Player player)) {
                return;
            }

            ItemStack stack = player.getMainHandItem();

            if (!isInscribable(stack)) {
                return;
            }

            capability.requestInscription(useEvent.user());
        }

        private static boolean isInscribable(ItemStack stack) {
            return stack.is(ModTags.Items.GHOST_TOMBSTONE_INSCRIBABLE);
        }
    }
}
