package com.qidate.qisplan2.ghost.module.implementation.calling;

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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import static com.qidate.qisplan2.QisPlan2.MODID;

/**
 * 完整喊人鬼模块。
 * 行为选择并跟随目标；媒介校验目标与灵异阻隔；转身事实触发规则并提交裁决；
 * 跟随喊名能力与袭击能力只在统一执行器批准后操作世界。
 */
public final class CallingGhostModule implements GhostModule {
    public static final ResourceLocation ID = id("calling_ghost");

    // 原实现固定使用 30 强度袭击，迁移后作为模块初始值，后续随实例 intensity 变化。
    public static final double INITIAL_INTENSITY = 30.0D;
    private static final ResourceLocation FOLLOW = id("calling_ghost/follow_call");
    private static final ResourceLocation ATTACK = id("calling_ghost/attack");
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MODID, path); }
    @Override public ResourceLocation getId() { return ID; }
    @Override public GhostModuleInitialData getInitialData() { return new GhostModuleInitialData(INITIAL_INTENSITY, 0); }
    @Override public List<Goal> createEntityGoals(ModularGhostEntity host) {
        CallingGhostState.initialize(host);
        return List.of(new CallingGhostBehavior(host));
    }
    public static boolean isActive(ModularGhostEntity host) {
        return host.level() instanceof ServerLevel && host.isAlive() && !host.isCoffinNailed()
                && !host.isSupernaturallyStunned() && host.getModules().stream()
                .anyMatch(e -> e.id().equals(ID) && com.qidate.qisplan2.ghost.module.GhostModuleCombatState.available(e));
    }
    static boolean isTurnAround(float previous, float current) {
        return Math.abs(Mth.wrapDegrees(current - previous)) >= 45.0F;
    }
    static void followAndCall(ModularGhostEntity host) {
        if (isActive(host)) GhostAbilityExecutor.tryExecute(new GhostAbilityContext(
                host, ID, FOLLOW, GhostAbilityContext.Source.BEHAVIOR, null));
    }
    @Override public List<GhostMedium> getMedia() {
        return List.of(new GhostMedium() {
            @Override public ResourceLocation getId() { return id("calling_ghost/target_medium"); }
            @Override public boolean allows(GhostAbilityContext c) {
                if (!(c.host() instanceof ModularGhostEntity host) || !isActive(host)) return false;
                var player = CallingGhostTargets.current(host);
                if (player == null || !CallingGhostTargets.canTrackPlayer(host, player)) return false;
                if (c.abilityId().equals(FOLLOW)) return CallingGhostState.read(host).getBoolean("physicsOwned");
                return c.event() instanceof EntityTurnedEvent e && e.observer() == host
                        && e.entity() == player && isTurnAround(e.previousYaw(), e.currentYaw())
                        && host.canAttackPlayer(player);
            }
        });
    }
    @Override public List<GhostRule> getRules() {
        return List.of(new GhostRule() {
            @Override public ResourceLocation getId() { return id("calling_ghost/turn_rule"); }
            @Override public boolean supports(GhostEvent e) {
                return e instanceof EntityTurnedEvent || e instanceof ModuleTickEvent;
            }
            @Override public boolean canTrigger(GhostRuleContext c, GhostEvent e) {
                return c.host() instanceof ModularGhostEntity host && isActive(host);
            }
            @Override public void onTrigger(GhostRuleContext c, GhostEvent e) {
                if (e instanceof ModuleTickEvent) {
                    CallingGhostState.tickTargetCooldown((ModularGhostEntity)c.host());
                } else if (e instanceof EntityTurnedEvent turn && isTurnAround(turn.previousYaw(), turn.currentYaw())) {
                    c.submitAbilityInfluence(ATTACK, turn, turn.entity().getUUID(),
                            GhostInfluenceType.ATTACK, c.getModuleContext().intensity());
                }
            }
        });
    }
    @Override public List<GhostAbility> getAbilities() {
        return List.of(new GhostAbility() {
            @Override public ResourceLocation getId() { return FOLLOW; }
            @Override public boolean canExecute(GhostAbilityContext c) { return c.host() instanceof ModularGhostEntity; }
            @Override public void execute(GhostAbilityContext c) {
                ModularGhostEntity host = (ModularGhostEntity)c.host();
                var player = CallingGhostTargets.current(host);
                if (player == null) return;
                Vec3 look = player.getLookAngle();
                Vec3 destination = player.position().subtract(look.x * 2.5D, 0, look.z * 2.5D).add(0, 1, 0);
                Vec3 movement = destination.subtract(host.position());
                double distance = movement.length();
                host.setDeltaMovement(distance < 0.1D ? Vec3.ZERO
                        : movement.normalize().scale(Math.min(distance * 0.5D, 0.35D)));
                host.setYRot(player.getYRot());
                host.setYBodyRot(player.getYRot());

                var data = CallingGhostState.read(host);
                int cooldown = Math.max(0, data.getInt("callCooldown"));
                if (cooldown > 0) {
                    data.putInt("callCooldown", cooldown - 1);
                    CallingGhostState.write(host, data);
                    return;
                }
                if (data.getInt("callCount") >= 7) {
                    CallingGhostState.clear(host);
                    host.setDeltaMovement(Vec3.ZERO);
                    return;
                }
                var sound = CallingGhostSounds.getSound(player);
                data.putInt("callCooldown", 100);
                if (sound != null) {
                    data.putInt("callCount", Math.max(0, data.getInt("callCount")) + 1);
                    host.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            sound, SoundSource.HOSTILE, 1.0F, 1.0F);
                }
                CallingGhostState.write(host, data);
            }
        }, new GhostAbility() {
            @Override public ResourceLocation getId() { return ATTACK; }
            @Override public boolean canExecute(GhostAbilityContext c) {
                return c.event() instanceof EntityTurnedEvent e && e.entity() instanceof ServerPlayer;
            }
            @Override public void execute(GhostAbilityContext c) { executeWithResult(c); }
            @Override public boolean executeWithResult(GhostAbilityContext c) {
                var event = (EntityTurnedEvent)c.event();
                return SupernaturalDeathHandler.tryKill(event.entity(),
                        ModDamageTypes.callingGhost((ModularGhostEntity)c.host()), c.getModuleContext().intensity());
            }
        });
    }
}
