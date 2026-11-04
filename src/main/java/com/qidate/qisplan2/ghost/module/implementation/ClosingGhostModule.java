package com.qidate.qisplan2.ghost.module.implementation;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.GhostAbility;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.DoorChangedEvent;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 关门鬼的完整模块定义。
 *
 * 该模块自身代表一只完整的关门鬼：规则识别关门事件，媒介
 * 限制袭击必须由有效门事件触发，能力负责瞬移和裁决后的袭击。
 */
public final class ClosingGhostModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MODID, "closing_ghost");
    public static final double INITIAL_INTENSITY = 5.0D;
    private static final double TRIGGER_RADIUS = 100.0D;
    private static final ResourceLocation MEDIUM_ID = ResourceLocation.fromNamespaceAndPath(MODID, "closing_ghost/door");
    private static final ResourceLocation RULE_ID = ResourceLocation.fromNamespaceAndPath(MODID, "closing_ghost/close_door_rule");
    private static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MODID, "closing_ghost/door_attack");
    private static final List<GhostMedium> MEDIA = List.of(new CloseDoorMedium());
    private static final List<GhostRule> RULES = List.of(new CloseDoorRule());
    private static final List<GhostAbility> ABILITIES = List.of(new DoorAttackAbility());

    @Override public ResourceLocation getId() { return ID; }
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(INITIAL_INTENSITY, 0.0D); }
    @Override public double getSupernaturalDefense() { return 6.0D; }
    @Override public List<GhostMedium> getMedia() { return MEDIA; }
    @Override public List<GhostRule> getRules() { return RULES; }
    @Override public List<GhostAbility> getAbilities() { return ABILITIES; }

    /** 只接受门从打开变为关闭的事件，避免其他方块变化触发关门鬼规则。 */
    private static boolean isCloseEvent(GhostEvent event) {
        return event instanceof DoorChangedEvent doorEvent && !doorEvent.opening();
    }
    /**
     * 校验门事件的来源与方块状态。规则只在服务端处理仍存活的触发者，
     * 并确认事件确实对应带朝向属性的门方块，避免无效事件进入裁决流程。
     */
    private static boolean isValidDoorEvent(DoorChangedEvent event) {
        return event.source().isAlive() && event.source().level() instanceof ServerLevel
                && event.doorState().getBlock() instanceof DoorBlock
                && event.doorState().hasProperty(DoorBlock.FACING);
    }
    /** 检查宿主是否存活、未死机，且仍在门事件的触发范围内。 */
    private static boolean isInRange(GhostModuleHost host, DoorChangedEvent event) {
        return host instanceof ModularGhostEntity ghost && ghost.isAlive()
                && !ghost.isSupernaturallyStunned()
                && ghost.distanceToSqr(event.doorPos().getX() + 0.5D,
                event.doorPos().getY() + 0.5D, event.doorPos().getZ() + 0.5D)
                <= TRIGGER_RADIUS * TRIGGER_RADIUS;
    }

    private static final class CloseDoorMedium implements GhostMedium {
        @Override public ResourceLocation getId() { return MEDIUM_ID; }
        @Override public boolean allows(GhostAbilityContext context) {
            // 媒介是能力执行前的硬性门槛：只有活着的玩家在服务端关闭有效门，
            // 且模块化宿主仍存活、未处于死机状态并处于触发范围内时，能力才可执行。
            return context.event() instanceof DoorChangedEvent event && isCloseEvent(event)
                    && isValidDoorEvent(event) && isInRange(context.host(), event);
        }
    }
    private static final class CloseDoorRule implements GhostRule {
        @Override public ResourceLocation getId() { return RULE_ID; }
        @Override public boolean supports(GhostEvent event) { return isCloseEvent(event); }
        @Override public boolean canTrigger(GhostRuleContext context, GhostEvent event) {
            return event instanceof DoorChangedEvent doorEvent && isCloseEvent(event)
                    && isValidDoorEvent(doorEvent) && isInRange(context.host(), doorEvent);
        }
        @Override public void onTrigger(GhostRuleContext context, GhostEvent event) {
            if (!(event instanceof DoorChangedEvent doorEvent)) return;
            // 规则只提交袭击影响，不直接杀死触发者。裁决系统会汇总同一事件内
            // 所有模块的影响并统一计算结果；只有该能力通过裁决后才会瞬移并执行袭击。
            context.submitAbilityInfluence(ABILITY_ID, event, doorEvent.source().getUUID(),
                    GhostInfluenceType.ATTACK, context.getModuleContext().intensity());
        }
    }
    private static final class DoorAttackAbility implements GhostAbility {
        @Override public ResourceLocation getId() { return ABILITY_ID; }
        @Override public boolean canExecute(GhostAbilityContext context) {
            return context.event() instanceof DoorChangedEvent event && isCloseEvent(event)
                    && isValidDoorEvent(event) && isInRange(context.host(), event);
        }
        @Override public void execute(GhostAbilityContext context) { executeWithResult(context); }
        @Override public boolean executeWithResult(GhostAbilityContext context) {
            if (!context.isModuleAvailable() || !(context.host() instanceof ModularGhostEntity ghost)
                    || !(context.event() instanceof DoorChangedEvent event) || !canExecute(context)) return false;
            // 通过媒介与裁决后才执行世界操作：先移动到触发门附近，再尝试灵异秒杀。
            teleportToDoorSide(ghost, event);
            LivingEntity source = event.source();
            return SupernaturalDeathHandler.tryKill(source, ModDamageTypes.closingGhost(ghost),
                    context.getModuleContext().intensity());
        }
    }
    private static void teleportToDoorSide(ModularGhostEntity ghost, DoorChangedEvent event) {
        // 使用门的朝向和触发者相对门中心的位置判断门的哪一侧，
        // 将厉鬼放到该侧门边；随后停止寻路与原有攻击目标，并转向触发者，
        // 避免瞬移完成后继续执行旧目标的移动或攻击行为。
        BlockState doorState = event.doorState();
        Direction facing = doorState.getValue(DoorBlock.FACING);
        double dx = event.source().getX() - (event.doorPos().getX() + 0.5D);
        double dz = event.source().getZ() - (event.doorPos().getZ() + 0.5D);
        int sideSign = dx * facing.getStepX() + dz * facing.getStepZ() >= 0.0D ? 1 : -1;
        ghost.teleportTo(event.doorPos().getX() + 0.5D + facing.getStepX() * sideSign * 0.8D,
                Math.max(event.doorPos().getY(), event.source().getY()),
                event.doorPos().getZ() + 0.5D + facing.getStepZ() * sideSign * 0.8D);
        ghost.getLookControl().setLookAt(event.source(), 30.0F, 30.0F);
        ghost.getNavigation().stop();
        ghost.setTarget(null);
        ghost.setAggressive(false);
    }
}
