package com.qidate.qisplan2.ghost.module.implementation.mist;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.entity.ai.GhostWanderGoal;
import com.qidate.qisplan2.ghost.domain.*;
import com.qidate.qisplan2.ghost.domain.type.mist.GhostMistDomainController;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostDomainData;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import static com.qidate.qisplan2.QisPlan2.MODID;

/** 完整鬼雾模块：独立维护鬼域生命周期，游荡参与行为调度，鬼域袭击经过统一裁决。 */
public final class GhostMistModule implements GhostModule {
    public static final ResourceLocation ID = id("ghost_mist");
    public static final double INITIAL_INTENSITY = 5;
    private static final ResourceLocation DOMAIN = id("ghost_mist/domain");
    private static final ResourceLocation ATTACK = id("ghost_mist/attack");
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MODID, path); }
    @Override public ResourceLocation getId() { return ID; }
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(INITIAL_INTENSITY, 0); }

    public static boolean isActive(ModularGhostEntity host) {
        return host.level() instanceof ServerLevel && host.isAlive() && !host.isRemoved()
                && !host.isCoffinNailed() && !host.isSupernaturallyStunned()
                && host.getModules().stream().anyMatch(e -> e.id().equals(ID) && GhostModuleCombatState.available(e));
    }
    @Override public List<Goal> createEntityGoals(ModularGhostEntity host) {
        GhostMistState.initialize(host);
        return List.of(new GhostWanderGoal(host, 0.7D) {
            @Override public boolean canUse() { return isActive(host) && super.canUse(); }
            @Override public boolean canContinueToUse() { return isActive(host) && super.canContinueToUse(); }
        });
    }
    /** 生命周期不依赖 MOVE 控制，也不依赖被死机过滤的规则 Tick。 */
    @Override public void onEntityTick(ModularGhostEntity host) {
        if (!isActive(host)) {
            onEntityDetached(host);
            return;
        }
        GhostAbilityExecutor.tryExecute(new GhostAbilityContext(host, ID, DOMAIN,
                GhostAbilityContext.Source.BEHAVIOR, null));
    }
    @Override public void onEntityDetached(ModularGhostEntity host) {
        if (host.level() instanceof ServerLevel level) {
            // 只移除该宿主的鬼雾类型，保留其他模块可能拥有的鬼域。
            GhostDomainManager.get(level).removeBySourceAndType(host.getUUID(), GhostMistDomainController.DOMAIN_TYPE);
        }
    }
    private static GhostDomain current(ModularGhostEntity host) {
        return GhostDomainManager.get((ServerLevel)host.level())
                .getBySourceAndType(host.getUUID(), GhostMistDomainController.DOMAIN_TYPE);
    }
    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            @Override public ResourceLocation getId() { return id("ghost_mist/domain_medium"); }
            @Override public boolean allows(GhostAbilityContext c) {
                var source = GhostModuleHosts.living(c.host());
                if (!GhostModuleHosts.available(c.host(), ID) || !(source.level() instanceof ServerLevel level)) return false;
                if (c.host() instanceof PlayerGhostModuleHost && c.event() instanceof GhostDomainExposureEvent e) {
                    var domain = GhostDomainManager.get(level).getBySourceAndType(source.getUUID(), ID);
                    return e.source() == source && e.target().isAlive() && e.target() != source
                            && domain != null && domain.getId().equals(e.domainId())
                            && GhostDomainEntityTracker.get(level).getEffectiveEntities(domain).contains(e.target());
                }
                if (!(c.host() instanceof ModularGhostEntity host) || !isActive(host)) return false;
                if (c.abilityId().equals(DOMAIN)) return true;
                if (!(c.event() instanceof GhostDomainExposureEvent e) || e.source() != host
                        || !e.target().isAlive() || e.target() == host || e.target().level() != host.level()) return false;
                GhostDomain domain = current(host);
                return domain != null && domain.getId().equals(e.domainId())
                        && GhostDomainEntityTracker.get((ServerLevel)host.level()).getEffectiveEntities(domain).contains(e.target());
            }
        });
    }
    @Override public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            @Override public ResourceLocation getId() { return id("ghost_mist/exposure_rule"); }
            @Override public boolean supports(GhostEvent e) { return e instanceof GhostDomainExposureEvent; }
            @Override public boolean canTrigger(GhostRuleContext c, GhostEvent e) {
                return GhostModuleHosts.available(c.host(), ID);
            }
            @Override public void onTrigger(GhostRuleContext c, GhostEvent event) {
                var e = (GhostDomainExposureEvent)event;
                c.submitAbilityInfluence(ATTACK, e, e.target().getUUID(),
                        GhostInfluenceType.ATTACK, e.domainStrength() / 5.0D);
            }
        });
    }
    @Override public List<GhostAbility> getIntrinsicAbilities() {
        return List.of(new GhostAbility() {
            @Override public ResourceLocation getId() { return DOMAIN; }
            @Override public boolean canExecute(GhostAbilityContext c) { return c.host() instanceof ModularGhostEntity; }
            @Override public void execute(GhostAbilityContext c) {
                var host = (ModularGhostEntity)c.host();
                GhostMistDomainController.createDomain((ServerLevel)host.level(), host);
            }
        }, new GhostAbility() {
            @Override public ResourceLocation getId() { return ATTACK; }
            @Override public boolean canExecute(GhostAbilityContext c) { return c.event() instanceof GhostDomainExposureEvent; }
            @Override public void execute(GhostAbilityContext c) { executeWithResult(c); }
            @Override public boolean executeWithResult(GhostAbilityContext c) {
                var source = GhostModuleHosts.living(c.host());
                var e = (GhostDomainExposureEvent)c.event();
                boolean killed = SupernaturalDeathHandler.tryKill(e.target(),
                        ModDamageTypes.ghostMist(source), e.domainStrength() / 5.0D);
                var level = (ServerLevel)source.level();
                var domain = GhostDomainManager.get(level).getBySourceAndType(source.getUUID(), ID);
                if (killed && domain != null && GhostModuleHosts.available(c.host(), ID)) {
                    if (source instanceof ModularGhostEntity entity) GhostMistDomainController.grow(level, entity, domain);
                    else if (source instanceof ServerPlayer player) GhostMistDomainController.grow(player, domain);
                }
                return killed;
            }
        });
    }


    /**
     * 基础强度。
     */


    @Override
    public double minimumStrengthRatio() {
        return 0.50D;
    }

    /**
     * 鬼域。
     */
    @Override
    public Optional<PossessedGhostDomainData> initialDomainData() {
        return Optional.of(
                PossessedGhostDomainData.create(
                        getInitialData().intensity(),
                        80.0D
                )
        );
    }

    /**
     * 每 tick 驱动鬼雾鬼域。
     */
    @Override
    public void onPlayerTick(
            PlayerGhostModuleContext context
    ) {

        GhostMistDomainController.tick(
                context
        );
    }

    /**
     * 解除驾驭时关闭鬼雾鬼域。
     */
    @Override
    public void onRelease(
            PlayerGhostModuleContext context
    ) {

        GhostMistDomainController.close(
                context.player()
        );
    }

}
