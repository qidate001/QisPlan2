package com.qidate.qisplan2.ghost.module.host.player.binding;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.corrosion.CorrosionType;
import com.qidate.qisplan2.ghost.module.*;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.event.*;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
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
import static com.qidate.qisplan2.QisPlan2.MODID;
import com.qidate.qisplan2.ghost.module.implementation.nightwanderer.NightWandererModule;

/** 玩家承载方式的生命周期和输入适配，不声明厉鬼身份、规则或初始数据。 */
public final class NightWandererPlayerBinding implements GhostPlayerBinding {
    private static final ResourceLocation ID = NightWandererModule.ID;
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


    /**
     * 推进玩家载体上的持续状态或表现，持久数据仍通过模块入口保存。
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

    /**
     * 推进夜游鬼驾驭状态：夜间累积自然复苏，白天累积浅死机。
     * 已处于死机时跳过这些增长，变化通过玩家模块状态入口写回。
     */
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

    /**
     * 把主动使用输入转交相应模块能力或适配逻辑。
     * @return 本次输入是否得到成功处理
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

        var ability = new GhostAbilityContext(context.host(), ID, NightWandererModule.ATTACK,
                GhostAbilityContext.Source.POSSESSION, null, () -> {}, new GhostTargetParameters(target));
        if (GhostAbilityExecutor.prepareDeferred(ability) == null) return false;

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

return GhostAbilityExecutor.tryExecute(new GhostAbilityContext(context.host(), ID,
                NightWandererModule.ATTACK, GhostAbilityContext.Source.POSSESSION, null,
                () -> {}, new GhostTargetParameters(target)));

    }

    /**
     * 在玩家离线或模块暂停时清理对应的运行时资源。
     * 清理不应移除模块的持久化数据。
     */
    @Override public void onPlayerSuspended(PlayerGhostModuleContext context) { removeEffects(context.player()); }

    /**
     * 处理解除驾驭后的表现或鬼域清理。
     */
    @Override
    public void onRelease(
            PlayerGhostModuleContext context
    ) {
        removeEffects(
                context.player()
        );
    }

    /**
     * 根据昼夜、方块光和天空光更新玩家视效与移动速度修饰。
     * 切换环境时移除相反效果，避免明暗速度修饰同时叠加。
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

    /**
     * 清理夜游鬼施加的夜视、失明和速度修饰，供暂停或解除驾驭使用。
     */
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

    /**
     * 移除夜游鬼暗处速度修饰，保留其他来源的属性修饰。
     */
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

    /**
     * 移除夜游鬼亮处速度修饰，保留其他来源的属性修饰。
     */
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
