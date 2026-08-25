package com.qidate.qisplan2.ghost.ability.nightwanderer;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.GhostAbilityContext;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LightLayer;

public final class NightWandererAbility
        implements com.qidate.qisplan2.ghost.ability.PossessedGhostAbility {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "night_wanderer"
            );

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

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /*
     * ============================================================
     * 驾驭后的持续能力
     * ============================================================
     */

    @Override
    public void tick(
            GhostAbilityContext context
    ) {
        tickNightWandererState(
                context
        );

        updateEffects(
                context.player()
        );
    }

    private void tickNightWandererState(
            GhostAbilityContext context
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
                            state,
                            NIGHT_REVIVAL_PER_TICK
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
        PossessionHandler.addShallowStun(
                player,
                id(),
                DAY_SHALLOW_STUN_PER_TICK
        );
    }

    /*
     * ============================================================
     * 主动能力
     * ============================================================
     */

    @Override
    public boolean use(
            GhostAbilityContext context
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
                        state,
                        revivalGain
                );

        /*
         * 记录本次使用时间。
         */
        newState =
                new PossessedGhostState(
                        newState.revival(),
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
         * 夜游鬼灵异攻击
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

        SupernaturalDeathHandler.tryKill(
                target,
                ModDamageTypes.ghostNightWanderer(
                        player
                ),
                4.0D
        );

        return true;
    }

    /*
     * ============================================================
     * 解除驾驭
     * ============================================================
     */

    @Override
    public void onRelease(
            GhostAbilityContext context
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