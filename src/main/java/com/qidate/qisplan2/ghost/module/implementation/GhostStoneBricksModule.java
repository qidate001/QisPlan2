package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRule;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.BlockDestructionAttemptEvent;
import com.qidate.qisplan2.ghost.module.event.DestructionAttemptEvent;
import com.qidate.qisplan2.ghost.module.event.EntityDestructionAttemptEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.ItemDestructionAttemptEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 完整的鬼石砖模块：方块、物品或实体即将被摧毁时反击破坏者。
 * 只有灵异反击实际成功，才取消当前这一次摧毁；裁决失败或攻击被抵挡不会保护宿主。
 */
public final class GhostStoneBricksModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MODID, "ghost_stone_bricks");
    private static final ResourceLocation RULE_ID = ResourceLocation.fromNamespaceAndPath(MODID, "ghost_stone_bricks/destruction_rule");
    private static final ResourceLocation BLOCK_ABILITY = ResourceLocation.fromNamespaceAndPath(MODID, "ghost_stone_bricks/block_destruction");
    private static final ResourceLocation ITEM_ABILITY = ResourceLocation.fromNamespaceAndPath(MODID, "ghost_stone_bricks/item_destruction");
    private static final ResourceLocation ENTITY_ABILITY = ResourceLocation.fromNamespaceAndPath(MODID, "ghost_stone_bricks/entity_destruction");
    private static final List<GhostAbility> ABILITIES = List.of(
            new RetaliationAbility(BLOCK_ABILITY, BlockDestructionAttemptEvent.class),
            new RetaliationAbility(ITEM_ABILITY, ItemDestructionAttemptEvent.class),
            new RetaliationAbility(ENTITY_ABILITY, EntityDestructionAttemptEvent.class));

    @Override public ResourceLocation getId() { return ID; }
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(5, 0); }
    @Override public List<GhostAbility> getIntrinsicAbilities() { return ABILITIES; }
    @Override public List<GhostRule> getRules() { return List.of(new DestructionRule()); }

    /** 每种摧毁事实都明确自己的反击对象；环境伤害没有对象时不执行反击。 */
    private static LivingEntity target(GhostEvent event) {
        if (event instanceof BlockDestructionAttemptEvent block) return block.getPlayer();
        if (event instanceof ItemDestructionAttemptEvent item) return item.getHolder();
        if (event instanceof EntityDestructionAttemptEvent entity) return entity.getAttacker();
        return null;
    }

    private static final class DestructionRule implements GhostRule {
        @Override public ResourceLocation getId() { return RULE_ID; }
        @Override public boolean supports(GhostEvent event) {
            return event instanceof BlockDestructionAttemptEvent
                    || event instanceof ItemDestructionAttemptEvent
                    || event instanceof EntityDestructionAttemptEvent;
        }
        @Override public boolean canTrigger(GhostRuleContext context, GhostEvent event) {
            return supports(event) && !((DestructionAttemptEvent)event).isCanceled()
                    && target(event) != null && target(event).isAlive();
        }
        @Override public void onTrigger(GhostRuleContext context, GhostEvent event) {
            ResourceLocation ability = event instanceof BlockDestructionAttemptEvent ? BLOCK_ABILITY
                    : event instanceof ItemDestructionAttemptEvent ? ITEM_ABILITY : ENTITY_ABILITY;
            context.submitAbilityInfluence(ability, event, target(event).getUUID(),
                    GhostInfluenceType.ATTACK, context.getModuleContext().intensity());
        }
    }

    /** 三种宿主共用同一条“实际反击成功才撤销摧毁”的能力实现。 */
    private record RetaliationAbility(ResourceLocation getId,
            Class<? extends DestructionAttemptEvent> eventType) implements GhostAbility {
        @Override public boolean canExecute(GhostAbilityContext context) {
            return eventType.isInstance(context.event())
                    && !((DestructionAttemptEvent)context.event()).isCanceled()
                    && target(context.event()) != null && target(context.event()).isAlive();
        }
        @Override public void execute(GhostAbilityContext context) { executeWithResult(context); }
        @Override public boolean executeWithResult(GhostAbilityContext context) {
            if (!canExecute(context)) return false;
            LivingEntity attacker = target(context.event());
            LivingEntity source = context.event() instanceof EntityDestructionAttemptEvent entity
                    ? entity.getEntity() : attacker;
            boolean succeeded = SupernaturalDeathHandler.tryKill(attacker,
                    ModDamageTypes.ghostStoneBricks(source), context.getModuleContext().intensity());
            if (succeeded) ((DestructionAttemptEvent)context.event()).cancel();
            return succeeded;
        }
    }
}
