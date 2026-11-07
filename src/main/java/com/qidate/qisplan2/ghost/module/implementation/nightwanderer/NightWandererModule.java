package com.qidate.qisplan2.ghost.module.implementation.nightwanderer;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.List;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 完整夜游鬼：夜间行为负责寻找和靠近目标，附近实体事实触发袭击规则。
 * 媒介限制夜间、距离、冷却与有效目标；袭击必须通过灵异裁决。
 * 击杀数和各项计时由 NightWandererState 写入模块 customData。
 */
public final class NightWandererModule implements GhostModule {
    public static final ResourceLocation ID = id("night_wanderer");
    public static final double INITIAL_INTENSITY = 5.0D;
    private static final ResourceLocation ACTIVITY = id("night_wanderer/activity");
    private static final ResourceLocation ATTACK = id("night_wanderer/attack");
    private static final ResourceLocation GROWTH = id("night_wanderer/sleep_growth");
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MODID, path); }

    @Override public ResourceLocation getId() { return ID; }
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(INITIAL_INTENSITY, 0); }
    @Override public double getSupernaturalDefense() { return 6; }
    @Override public List<Goal> createEntityGoals(ModularGhostEntity host) {
        NightWandererState.initialize(host);
        return List.of(new NightWandererBehavior(host));
    }

    public static boolean isActive(ModularGhostEntity host) {
        return host.level() instanceof ServerLevel && host.isAlive() && !host.isCoffinNailed()
                && !host.isSupernaturallyStunned() && host.getModules().stream()
                .anyMatch(e -> e.id().equals(ID) && !e.sleeping() && !e.deadlocked());
    }

    /** 实体行为直接调用活动能力，不以请求伪装语义事件。 */
    static void activity(ModularGhostEntity host) {
        if (isActive(host)) GhostAbilityExecutor.tryExecute(new GhostAbilityContext(
                host, ID, ACTIVITY, GhostAbilityContext.Source.BEHAVIOR, null));
    }

    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            @Override public ResourceLocation getId() { return id("night_wanderer/medium"); }
            @Override public boolean allows(GhostAbilityContext c) {
                if (!(c.host() instanceof ModularGhostEntity host) || !isActive(host)) return false;
                if (c.abilityId().equals(GROWTH)) return c.event() instanceof SleepTimeSkippedEvent e && e.skippedTicks() >= 600;
                if (host.level().isDay()) return false;
                if (c.abilityId().equals(ACTIVITY)) return true;
                return c.event() instanceof EntityNearbyEvent e && e.observer() == host
                        && NightWandererBehavior.validTarget(host, e.target())
                        && host.distanceToSqr(e.target()) <= 9
                        && NightWandererState.get(host, NightWandererState.ATTACK_COOLDOWN) == 0;
            }
        });
    }

    @Override public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            @Override public ResourceLocation getId() { return id("night_wanderer/rules"); }
            @Override public boolean supports(GhostEvent e) {
                return e instanceof EntityNearbyEvent || e instanceof SleepTimeSkippedEvent;
            }
            @Override public boolean canTrigger(GhostRuleContext c, GhostEvent e) {
                return c.host() instanceof ModularGhostEntity host && isActive(host);
            }
            @Override public void onTrigger(GhostRuleContext c, GhostEvent e) {
                if (e instanceof EntityNearbyEvent nearby) {
                    c.submitAbilityInfluence(ATTACK, e, nearby.target().getUUID(),
                            GhostInfluenceType.ATTACK, c.getModuleContext().intensity());
                } else {
                    GhostAbilityExecutor.tryExecute(new GhostAbilityContext(c.host(), ID, GROWTH,
                            GhostAbilityContext.Source.RULE, e));
                }
            }
        });
    }

    @Override public List<GhostAbility> getAbilities() {
        return List.of(new GhostAbility() {
            @Override public ResourceLocation getId() { return ACTIVITY; }
            @Override public boolean canExecute(GhostAbilityContext c) { return c.host() instanceof ModularGhostEntity; }
            @Override public void execute(GhostAbilityContext c) {
                NightWandererBehavior.tickActivity((ModularGhostEntity)c.host());
            }
        }, new GhostAbility() {
            @Override public ResourceLocation getId() { return ATTACK; }
            @Override public boolean canExecute(GhostAbilityContext c) { return c.event() instanceof EntityNearbyEvent; }
            @Override public void execute(GhostAbilityContext c) { executeWithResult(c); }
            @Override public boolean executeWithResult(GhostAbilityContext c) {
                ModularGhostEntity host = (ModularGhostEntity)c.host();
                var target = ((EntityNearbyEvent)c.event()).target();
                host.getNavigation().stop();
                host.swing(InteractionHand.MAIN_HAND);
                boolean killed = SupernaturalDeathHandler.tryKill(target,
                        ModDamageTypes.ghostNightWanderer(host), c.getModuleContext().intensity());
                if (killed) NightWandererState.addKills(host, 1);
                NightWandererState.startAttackCooldown(host);
                return killed;
            }
        }, new GhostAbility() {
            @Override public ResourceLocation getId() { return GROWTH; }
            @Override public boolean canExecute(GhostAbilityContext c) { return c.event() instanceof SleepTimeSkippedEvent; }
            @Override public void execute(GhostAbilityContext c) {
                long ticks = ((SleepTimeSkippedEvent)c.event()).skippedTicks();
                NightWandererState.addKills((ModularGhostEntity)c.host(),
                        (int)Math.min(Integer.MAX_VALUE, ticks / 600));
            }
        });
    }
}
