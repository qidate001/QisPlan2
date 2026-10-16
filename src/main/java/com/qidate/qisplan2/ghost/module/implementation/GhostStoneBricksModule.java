package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.BlockDestructionAttemptEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.ItemDestructionAttemptEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

import static com.qidate.qisplan2.QisPlan2.MODID;

public final class GhostStoneBricksModule implements GhostModule {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_stone_bricks"
            );

    private static final ResourceLocation RULE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_stone_bricks/destruction_rule"
            );

    private static final ResourceLocation BLOCK_DESTRUCTION_ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_stone_bricks/block_destruction"
            );

    private static final ResourceLocation ITEM_DESTRUCTION_ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MODID,
                    "ghost_stone_bricks/item_destruction"
            );

    private static final List<GhostRule> RULES =
            List.of(new GhostStoneBricksRule());

    private static final List<GhostAbility> ABILITIES =
            List.of(
                    new BlockDestructionAbility(),
                    new ItemDestructionAbility()
            );

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    /**
     * 定义鬼石砖模块的新实例初始属性。
     */
    @Override
    public GhostModuleInitialData getInitialData() {
        return new GhostModuleInitialData(
                5.0D,
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

    private void handleBlockDestruction(
            GhostModuleContext context,
            BlockDestructionAttemptEvent event
    ) {
        Player player = event.getPlayer();

        boolean killed = SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostStoneBricks(player),
                context.intensity()
        );

        if (killed) {
            event.cancel();
        }
    }

    private void handleItemDestruction(
            GhostModuleContext context,
            ItemDestructionAttemptEvent event
    ) {
        if (!(event.getHolder() instanceof Player player)) {
            return;
        }

        boolean killed = SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostStoneBricks(player),
                context.intensity()
        );

        if (killed) {
            event.cancel();
        }
    }

    /**
     * 鬼石砖的规则。
     *
     * 负责识别破坏事件，并选择对应能力。
     */
    private static final class GhostStoneBricksRule
            implements GhostRule {

        @Override
        public ResourceLocation getId() {
            return RULE_ID;
        }

        @Override
        public boolean supports(GhostEvent event) {
            return event instanceof BlockDestructionAttemptEvent
                    || event instanceof ItemDestructionAttemptEvent;
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
            if (event instanceof BlockDestructionAttemptEvent) {
                context.tryExecuteAbility(BLOCK_DESTRUCTION_ABILITY_ID, event);
                return;
            }

            if (event instanceof ItemDestructionAttemptEvent) {
                context.tryExecuteAbility(ITEM_DESTRUCTION_ABILITY_ID, event);
            }
        }
    }

    /**
     * 方块破坏能力。
     */
    private static final class BlockDestructionAbility
            implements GhostAbility {

        @Override
        public ResourceLocation getId() {
            return BLOCK_DESTRUCTION_ABILITY_ID;
        }

        @Override
        public boolean canExecute(GhostAbilityContext context) {
            return context.event() instanceof BlockDestructionAttemptEvent;
        }

        @Override
        public void execute(GhostAbilityContext context) {
            BlockDestructionAttemptEvent event =
                    (BlockDestructionAttemptEvent) context.event();

            Player player = event.getPlayer();

            boolean killed = SupernaturalDeathHandler.tryKill(
                    player,
                    ModDamageTypes.ghostStoneBricks(player),
                    context.getModuleContext().intensity()
            );

            if (killed) {
                event.cancel();
            }
        }
    }

    /**
     * 物品耐久耗尽能力。
     */
    private static final class ItemDestructionAbility
            implements GhostAbility {

        @Override
        public ResourceLocation getId() {
            return ITEM_DESTRUCTION_ABILITY_ID;
        }

        @Override
        public boolean canExecute(GhostAbilityContext context) {
            return context.event() instanceof ItemDestructionAttemptEvent event
                    && event.getHolder() instanceof Player;
        }

        @Override
        public void execute(GhostAbilityContext context) {
            ItemDestructionAttemptEvent event =
                    (ItemDestructionAttemptEvent) context.event();

            Player player = (Player) event.getHolder();

            boolean killed = SupernaturalDeathHandler.tryKill(
                    player,
                    ModDamageTypes.ghostStoneBricks(player),
                    context.getModuleContext().intensity()
            );

            if (killed) {
                event.cancel();
            }
        }
    }
}