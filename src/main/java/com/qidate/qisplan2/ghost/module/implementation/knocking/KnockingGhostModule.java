package com.qidate.qisplan2.ghost.module.implementation.knocking;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.event.DoorKnockHeardEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.possession.ability.ghostdoor.knocking.KnockingGhostDoorSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.List;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 完整的敲门鬼模块：行为负责找门与靠近，媒介限定有效门和敲门声事件，
 * 听声规则提交延迟袭击裁决，能力负责敲门、鬼门传播和获准后的灵异袭击。
 */
public final class KnockingGhostModule implements GhostModule {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost");
    public static final double INITIAL_INTENSITY = 5.0D;
    private static final ResourceLocation KNOCK = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/knock");
    private static final ResourceLocation ATTACK = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/attack");
    private static final ResourceLocation RULE = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/hearing");
    private static final ResourceLocation MEDIUM = ResourceLocation.fromNamespaceAndPath(MODID, "knocking_ghost/door_sound");

    @Override public ResourceLocation getId() { return ID; }
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(INITIAL_INTENSITY, 0); }
    @Override public double getSupernaturalDefense() { return 6; }
    @Override public List<Goal> createEntityGoals(ModularGhostEntity host) { return KnockingGhostBehavior.create(host); }

    /** 每次行为及延迟任务执行前重新检查，移除、休眠和死机立即阻止后续操作。 */
    public static boolean isActive(ModularGhostEntity host) {
        return host.isAlive() && !host.isSupernaturallyStunned()
                && host.getModules().stream().anyMatch(e -> e.id().equals(ID) && GhostModuleCombatState.available(e));
    }

    public static void knock(ModularGhostEntity host, BlockPos door) {
        if (isActive(host)) GhostAbilityExecutor.tryExecute(new GhostAbilityContext(
                host, ID, KNOCK, GhostAbilityContext.Source.BEHAVIOR, null, () -> {}, new DoorKnockParameters(door)));
    }

    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            @Override public ResourceLocation getId() { return MEDIUM; }
            @Override public boolean allows(GhostAbilityContext c) {
                if (!(c.host() instanceof ModularGhostEntity host) || !isActive(host)
                        || !(host.level() instanceof ServerLevel)) return false;
                if (c.parameters() instanceof DoorKnockParameters e) return c.abilityId().equals(KNOCK)
                        && KnockingGhostDoorSystem.isDoor(host.level(), e.door());
                return c.abilityId().equals(ATTACK) && c.event() instanceof DoorKnockHeardEvent e
                        && e.source() == host && e.target().isAlive() && e.target().level() == host.level();
            }
        });
    }

    @Override public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            @Override public ResourceLocation getId() { return RULE; }
            @Override public boolean supports(GhostEvent event) { return event instanceof DoorKnockHeardEvent; }
            @Override public boolean canTrigger(GhostRuleContext c, GhostEvent e) {
                return c.host() instanceof ModularGhostEntity host && isActive(host)
                        && e instanceof DoorKnockHeardEvent heard && heard.source() == host;
            }
            @Override public void onTrigger(GhostRuleContext c, GhostEvent event) {
                DoorKnockHeardEvent e = (DoorKnockHeardEvent) event;
                // 听见声音时立即处理规则并提交裁决。获准效果安排延迟能力调用，
                // 延迟结束不再制造事件；执行器会重新检查模块、媒介与能力。
                Runnable attack = GhostAbilityExecutor.prepareDeferred(new GhostAbilityContext(
                        c.host(), ID, ATTACK, GhostAbilityContext.Source.RULE, e));
                if (attack != null) c.submitInfluence(e.target().getUUID(), GhostInfluenceType.ATTACK,
                        c.getModuleContext().intensity(),
                        () -> KnockingGhostDoorSystem.scheduleModuleAttack(attack));
            }
        });
    }

    @Override public List<GhostAbility> getAbilities() {
        return List.of(new GhostAbility() {
            @Override public ResourceLocation getId() { return KNOCK; }
            @Override public boolean canExecute(GhostAbilityContext c) { return c.parameters() instanceof DoorKnockParameters; }
            @Override public void execute(GhostAbilityContext c) {
                ModularGhostEntity host = (ModularGhostEntity) c.host();
                KnockingGhostDoorSystem.knock((ServerLevel) host.level(), host,
                        ((DoorKnockParameters)c.parameters()).door(), c.getModuleContext().intensity());
            }
        }, new GhostAbility() {
            @Override public ResourceLocation getId() { return ATTACK; }
            @Override public boolean canExecute(GhostAbilityContext c) { return c.event() instanceof DoorKnockHeardEvent; }
            @Override public void execute(GhostAbilityContext c) { executeWithResult(c); }
            @Override public boolean executeWithResult(GhostAbilityContext c) {
                DoorKnockHeardEvent e = (DoorKnockHeardEvent)c.event();
                return SupernaturalDeathHandler.tryKill(e.target(),
                        ModDamageTypes.knockingGhost((LivingEntity)c.host()), c.getModuleContext().intensity());
            }
        });
    }
}
