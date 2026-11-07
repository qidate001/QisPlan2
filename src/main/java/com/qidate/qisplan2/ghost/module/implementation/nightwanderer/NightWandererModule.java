package com.qidate.qisplan2.ghost.module.implementation.nightwanderer;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.corrosion.GhostCorrosion;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.data.GhostModuleInitialData;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.LightLayer;
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
                .anyMatch(e -> e.id().equals(ID) && GhostModuleCombatState.available(e));
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

    @Override public List<GhostAbility> getIntrinsicAbilities() {
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


    /**
     * 10 秒。
     */
    private static final long TEN_SECONDS = 200L;

    /**
     * 第一次/正常使用：+10%
     */
    private static final double NORMAL_REVIVAL_GAIN = 10.0D;

    /**
     * 10 秒内再次使用：+30%
     */
    private static final double RAPID_REVIVAL_GAIN = 30.0D;

    /**
     * 夜晚自然复苏：
     * 0.1% / 秒
     */
    private static final double NIGHT_REVIVAL_PER_TICK =
            0.1D / 20.0D;

    /**
     * 白天浅死机：
     * 1 点 / 10 秒
     */
    private static final double DAY_SHALLOW_STUN_PER_TICK =
            1.0D / 200.0D;

    private static final double DARK_SPEED =
            0.45D;

    private static final double LIGHT_SPEED =
            -0.10D;

    private static final ResourceLocation DARK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "possessed_night_wanderer_dark_speed"
            );

    private static final ResourceLocation LIGHT_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "possessed_night_wanderer_light_speed"
            );

    private static final AttributeModifier DARK_SPEED_MODIFIER =
            new AttributeModifier(
                    DARK_SPEED_ID,
                    DARK_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );

    private static final AttributeModifier LIGHT_SPEED_MODIFIER =
            new AttributeModifier(
                    LIGHT_SPEED_ID,
                    LIGHT_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );

    private static final GhostCorrosion CORROSION =
            GhostCorrosion.builder()
                    .add(CorrosionType.GLOBAL, 20)
                    .add(CorrosionType.HAND, 20)
                    .add(CorrosionType.EYE, 20)
                    .add(CorrosionType.FOOT, 10)
                    .build();


    @Override
    public GhostCorrosion corrosion() {
        return CORROSION;
    }

    /*
     * ============================================================
     * 驾驭后的持续能力
     * ============================================================
     */

    @Override
    public void onPlayerTick(
            PlayerGhostModuleContext context
    ) {
        tickNightWandererState(
                context
        );

        updateEffects(
                context.player()
        );
    }

    private void tickNightWandererState(
            PlayerGhostModuleContext context
    ) {

        PossessedGhostState state =
                context.state();

        ServerPlayer player =
                context.player();

        /*
         * ========================================
         * 死机期间：
         *
         * 不复苏
         * 不增加浅死机
         * ========================================
         */
        if (state.isAnyStun()) {
            return;
        }

        /*
         * ========================================
         * 夜晚：
         *
         * 自然复苏 0.1% / 秒
         * ========================================
         */
        if (!player.level().isDay()) {

            PossessedGhostState newState =
                    PossessionHandler.addRevival(
                            player,
                            ID,
                            state,
                            NIGHT_REVIVAL_PER_TICK,
                            CorrosionType.GLOBAL
                    );

            context.setState(
                    newState
            );

            return;
        }

        /*
         * ========================================
         * 白天：
         *
         * 晒太阳 → 浅死机
         * 1 点 / 10 秒
         * ========================================
         */
        PossessedGhostState newState =
                PossessionHandler.addShallowStun(
                        state,
                        DAY_SHALLOW_STUN_PER_TICK
                );

        context.setState(
                newState
        );
    }

    /*
     * ============================================================
     * 主动能力
     * ============================================================
     */

    @Override
    public boolean use(
            PlayerGhostModuleContext context
    ) {

        ServerPlayer player =
                context.player();

        LivingEntity target =
                context.target();

        if (target == null
                || !target.isAlive()) {

            return false;
        }

        PossessedGhostState state =
                context.state();

        long now =
                player.serverLevel()
                        .getGameTime();

        /*
         * 10 秒内再次使用：
         * +30%
         *
         * 否则：
         * +10%
         */
        boolean rapid =
                now - state.lastAbilityUseTick()
                        < TEN_SECONDS;

        double revivalGain =
                rapid
                        ? RAPID_REVIVAL_GAIN
                        : NORMAL_REVIVAL_GAIN;

        /*
         * 使用通用复苏机制。
         */
        PossessedGhostState newState =
                PossessionHandler.addRevival(
                        player,
                        ID,
                        state,
                        revivalGain,
                        CorrosionType.HAND
                );

        /*
         * 记录本次使用时间。
         */
        newState =
                new PossessedGhostState(
                        newState.revival(),
                        state.strength(),
                        newState.shallowStun(),
                        newState.stunTicks(),
                        newState.permanentStun(),
                        now
                );

        context.setState(
                newState
        );

        /*
         * 复苏达到 100%：
         * 玩家死亡。
         */
        if (newState.revival()
                >= 1.0D) {

            player.setHealth(0.0F);

            return true;
        }

        /*
         * ========================================================
         * 音效 与 粒子效果
         * ========================================================
         */
        if (player.level()
                instanceof ServerLevel serverLevel) {

            serverLevel.sendParticles(
                    ParticleTypes.SOUL,
                    target.getX(),
                    target.getY()
                            + target.getBbHeight()
                            * 0.5D,
                    target.getZ(),
                    12,
                    0.35D,
                    0.45D,
                    0.35D,
                    0.03D
            );

            serverLevel.playSound(
                    null,
                    target.blockPosition(),
                    SoundEvents.SOUL_ESCAPE.value(),
                    SoundSource.HOSTILE,
                    0.8F,
                    0.7F
            );
        }

        /*
         * ========================================================
         * 灵异袭击
         * ========================================================
         */

        double attackStrength =
                PossessionHandler.getEffectiveStrength(
                        player,
                        ID
                );

        SupernaturalDeathHandler.tryKill(
                target,
                ModDamageTypes.ghostNightWanderer(
                        player
                ),
                attackStrength
        );

        return true;
    }

    /*
     * ============================================================
     * 解除驾驭
     * ============================================================
     */

    @Override public GhostInfluenceType playerActionInfluence() { return GhostInfluenceType.ATTACK; }
    @Override public void onPlayerSuspended(PlayerGhostModuleContext context) { removeEffects(context.player()); }

    @Override
    public void onRelease(
            PlayerGhostModuleContext context
    ) {
        removeEffects(
                context.player()
        );
    }

    /*
     * ============================================================
     * 夜游鬼环境效果
     * ============================================================
     */

    private void updateEffects(
            ServerPlayer player
    ) {

        int blockLight =
                player.level().getBrightness(
                        LightLayer.BLOCK,
                        player.blockPosition()
                );

        int skyLight =
                player.level().getBrightness(
                        LightLayer.SKY,
                        player.blockPosition()
                );

        boolean day =
                player.level().isDay();

        /*
         * ========================================
         * 白天 / 夜晚视觉效果
         * ========================================
         */

        if (day) {

            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.BLINDNESS,
                            40,
                            0,
                            false,
                            false,
                            true
                    )
            );

            player.removeEffect(
                    MobEffects.NIGHT_VISION
            );

        } else {

            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.NIGHT_VISION,
                            40,
                            0,
                            false,
                            false,
                            true
                    )
            );

            player.removeEffect(
                    MobEffects.BLINDNESS
            );
        }

        /*
         * ========================================
         * 移动速度
         * ========================================
         */

        AttributeInstance speed =
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (speed == null) {
            return;
        }

        /*
         * 暗处：
         *
         * 方块光 <= 3
         *
         * 并且：
         * 夜晚
         * 或天空光 <= 3
         */
        boolean dark =
                blockLight <= 3
                        && (!day || skyLight <= 3);

        /*
         * 亮处：
         *
         * 方块光 >= 8
         * 或白天天空光 >= 8
         */
        boolean bright =
                blockLight >= 8
                        || (day && skyLight >= 8);

        if (dark) {

            removeLightModifier(
                    speed
            );

            if (!speed.hasModifier(
                    DARK_SPEED_ID
            )) {

                speed.addTransientModifier(
                        DARK_SPEED_MODIFIER
                );
            }

        } else if (bright) {

            removeDarkModifier(
                    speed
            );

            if (!speed.hasModifier(
                    LIGHT_SPEED_ID
            )) {

                speed.addTransientModifier(
                        LIGHT_SPEED_MODIFIER
                );
            }

        } else {

            removeDarkModifier(
                    speed
            );

            removeLightModifier(
                    speed
            );
        }
    }

    private void removeEffects(
            ServerPlayer player
    ) {

        /*
         * 夜视 / 失明
         */
        player.removeEffect(
                MobEffects.NIGHT_VISION
        );

        player.removeEffect(
                MobEffects.BLINDNESS
        );

        AttributeInstance speed =
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (speed == null) {
            return;
        }

        removeDarkModifier(
                speed
        );

        removeLightModifier(
                speed
        );
    }

    private void removeDarkModifier(
            AttributeInstance attribute
    ) {
        if (attribute.hasModifier(
                DARK_SPEED_ID
        )) {

            attribute.removeModifier(
                    DARK_SPEED_ID
            );
        }
    }

    private void removeLightModifier(
            AttributeInstance attribute
    ) {
        if (attribute.hasModifier(
                LIGHT_SPEED_ID
        )) {

            attribute.removeModifier(
                    LIGHT_SPEED_ID
            );
        }
    }

}
