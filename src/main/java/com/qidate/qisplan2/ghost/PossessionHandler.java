package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LightLayer;

import java.util.HashMap;
import java.util.Map;

public final class PossessionHandler {

    private PossessionHandler() {
    }

    public static final ResourceLocation NIGHT_WANDERER =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "night_wanderer"
            );

    /**
     * 10 秒。
     */
    public static final long TEN_SECONDS = 200L;

    /**
     * 第一次/正常使用：+10%
     */
    public static final double NORMAL_REVIVAL_GAIN = 0.10D;

    /**
     * 10 秒内再次使用：+30%
     */
    public static final double RAPID_REVIVAL_GAIN = 0.30D;

    /**
     * 夜晚复苏速度：
     * 1% / 10 秒
     */
    private static final double NIGHT_REVIVAL_PER_TICK =
            0.01D / 200.0D;

    /**
     * 白天浅死机值增长：
     * 1 点 / 10 秒
     */
    private static final double DAY_SHALLOW_STUN_PER_TICK =
            1.0D / 200.0D;

    /**
     * 复苏值最大 100%
     */
    private static final double MAX_REVIVAL =
            1.0D;


    private static PossessedGhostState addRevival(
            PossessedGhostState state,
            double revivalPercent
    ) {
        if (revivalPercent <= 0.0D) {
            return state;
        }

        /*
         * ========================================
         * 死机状态
         * ========================================
         *
         * 死机期间：
         * 可以使用技能，
         * 但任何复苏增长都无效。
         *
         * 浅死机值也不会被消耗。
         */
        if (state.isAnyStun()) {
            return state;
        }

        double revival =
                state.revival();

        double shallowStun =
                state.shallowStun();


        /*
         * ========================================
         * 先由浅死机值抵消复苏增长
         *
         * 1 点浅死机
         * = 抵消 1% 复苏增长
         * ========================================
         */
        double consumed =
                Math.min(
                        shallowStun,
                        revivalPercent
                );

        shallowStun -= consumed;

        double actualRevival =
                revivalPercent - consumed;

        /*
         * ========================================
         * 浅死机值抵消不完的部分，
         * 才真正进入复苏值
         * ========================================
         */
        revival +=
                actualRevival / 100.0D;

        revival =
                Math.min(
                        1.0D,
                        revival
                );

        return new PossessedGhostState(
                revival,
                shallowStun,
                state.stunTicks(),
                state.permanentStun(),
                state.lastAbilityUseTick()
        );
    }


    /**
     * 驭鬼。
     */
    public static boolean possess(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        Map<ResourceLocation, PossessedGhostState> oldData =
                player.getData(QisPlan2.POSSESSED_GHOSTS);

        if (oldData.containsKey(ghost)) {
            return false;
        }

        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(oldData);

        data.put(
                ghost,
                PossessedGhostState.create()
        );

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );

        return true;
    }


    /**
     * 解除驭鬼。
     */
    public static boolean release(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        Map<ResourceLocation, PossessedGhostState> oldData =
                player.getData(QisPlan2.POSSESSED_GHOSTS);

        if (!oldData.containsKey(ghost)) {
            return false;
        }

        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(oldData);

        data.remove(ghost);

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );

        return true;
    }


    /**
     * 是否驾驭了某只鬼。
     */
    public static boolean hasGhost(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        return player.getData(
                QisPlan2.POSSESSED_GHOSTS
        ).containsKey(ghost);
    }


    /**
     * 获取某只鬼的状态。
     */
    public static PossessedGhostState getState(
            ServerPlayer player,
            ResourceLocation ghost
    ) {

        return player.getData(
                QisPlan2.POSSESSED_GHOSTS
        ).get(ghost);
    }

    public static boolean testStun(
            ServerPlayer player,
            ResourceLocation ghost,
            long ticks
    ) {
        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        player.getData(
                                QisPlan2.POSSESSED_GHOSTS
                        )
                );

        PossessedGhostState state =
                data.get(ghost);

        if (state == null) {
            return false;
        }

        data.put(
                ghost,
                new PossessedGhostState(
                        state.revival(),
                        state.shallowStun(),
                        Math.max(1L, ticks),
                        false,
                        state.lastAbilityUseTick()
                )
        );

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );

        return true;
    }


    public static boolean testPermanentStun(
            ServerPlayer player,
            ResourceLocation ghost
    ) {
        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        player.getData(
                                QisPlan2.POSSESSED_GHOSTS
                        )
                );

        PossessedGhostState state =
                data.get(ghost);

        if (state == null) {
            return false;
        }

        data.put(
                ghost,
                new PossessedGhostState(
                        state.revival(),
                        state.shallowStun(),
                        0L,
                        true,
                        state.lastAbilityUseTick()
                )
        );

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );

        return true;
    }


    public static void tick(
            ServerPlayer player
    ) {
        Map<ResourceLocation, PossessedGhostState> oldData =
                player.getData(QisPlan2.POSSESSED_GHOSTS);

        if (oldData.isEmpty()) {
            return;
        }

        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(oldData);

        boolean changed = false;

        boolean isDay =
                player.level().isDay();

        for (var entry : oldData.entrySet()) {

            ResourceLocation ghost =
                    entry.getKey();

            PossessedGhostState state =
                    entry.getValue();

            double revival =
                    state.revival();

            double shallowStun =
                    state.shallowStun();

            long stunTicks =
                    state.stunTicks();

            boolean permanentStun =
                    state.permanentStun();

            /*
             * ========================================
             * 永久死机
             * ========================================
             *
             * 完全不复苏。
             */
            if (permanentStun) {

                // 什么都不处理

            }
            /*
             * ========================================
             * 普通死机
             * ========================================
             *
             * 死机期间：
             * 不复苏
             * 不增加浅死机
             */
            else if (stunTicks > 0) {

                stunTicks--;

            }
            /*
             * ========================================
             * 正常状态
             * ========================================
             */
            else {

                /*
                 * 夜晚：
                 * 每秒自然复苏 0.1%
                 *
                 * 复苏增长先由浅死机抵消。
                 */
                if (!isDay) {

                    PossessedGhostState newState =
                            addRevival(
                                    state,
                                    0.1D / 20.0D
                            );

                    revival =
                            newState.revival();

                    shallowStun =
                            newState.shallowStun();
                }

                /*
                 * 白天：
                 * 不复苏
                 * 浅死机缓慢增加
                 */
                else {

                    shallowStun +=
                            DAY_SHALLOW_STUN_PER_TICK;
                }
            }

            /*
             * ========================================
             * 复苏达到 100%
             * ========================================
             */
            if (revival >= 1.0D) {

                player.setHealth(0.0F);

                return;
            }

            /*
             * 保存新的状态
             */
            PossessedGhostState newState =
                    new PossessedGhostState(
                            revival,
                            shallowStun,
                            stunTicks,
                            permanentStun,
                            state.lastAbilityUseTick()
                    );

            data.put(
                    ghost,
                    newState
            );

            /*
             * 判断是否变化
             */
            if (revival != state.revival()
                    || shallowStun != state.shallowStun()
                    || stunTicks != state.stunTicks()
                    || permanentStun != state.permanentStun()) {

                changed = true;
            }
        }

        if (changed) {
            player.setData(
                    QisPlan2.POSSESSED_GHOSTS,
                    data
            );
        }

        /*
         * ========================================
         * 夜游鬼能力
         * ========================================
         */
        if (hasGhost(
                player,
                NIGHT_WANDERER
        )) {
            updateNightWandererEffects(player);
        } else {
            removeNightWandererEffect(player);
        }
    }


    private static void removeNightWandererEffect(
            ServerPlayer player
    ) {
        /*
         * ========================================
         * 移除夜视/失明
         * ========================================
         */
        player.removeEffect(
                MobEffects.NIGHT_VISION
        );

        player.removeEffect(
                MobEffects.BLINDNESS
        );

        /*
         * ========================================
         * 移除夜游鬼移速修饰器
         * ========================================
         */
        AttributeInstance speed =
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (speed != null) {
            removeNightWandererDarkModifier(speed);
            removeNightWandererLightModifier(speed);
        }
    }


    /**
     * 使用当前鬼的能力。
     *
     * 现在暂时只有夜游鬼，
     * 所以直接使用夜游鬼。
     */
    public static boolean useNightWandererAbility(
            ServerPlayer player,
            LivingEntity target
    ) {
        ResourceLocation ghost = NIGHT_WANDERER;

        /*
         * ========================================
         * 检查是否驾驭夜游鬼
         * ========================================
         */
        PossessedGhostState state =
                getState(player, ghost);

        if (state == null) {
            return false;
        }

        /*
         * ========================================
         * 当前时间
         * ========================================
         */
        long now =
                player.serverLevel().getGameTime();

        /*
         * ========================================
         * 判断是不是 10 秒内再次使用
         *
         * 第一次：
         *     +10%
         *
         * 10 秒内再次使用：
         *     +30%
         * ========================================
         */
        boolean rapid =
                now - state.lastAbilityUseTick()
                        < TEN_SECONDS;

        double revivalGain =
                rapid
                        ? 30.0D
                        : 10.0D;

        /*
         * ========================================
         * 先通过统一方法处理复苏值
         *
         * 它会：
         *
         * 1. 先扣浅死机值
         * 2. 浅死机值不够时
         *    剩余部分才增加复苏值
         * ========================================
         */
        PossessedGhostState newState =
                addRevival(
                        state,
                        revivalGain
                );

        /*
         * ========================================
         * 更新本次使用时间
         * ========================================
         */
        newState =
                new PossessedGhostState(
                        newState.revival(),
                        newState.shallowStun(),
                        newState.stunTicks(),
                        newState.permanentStun(),
                        now
                );

        /*
         * ========================================
         * 保存夜游鬼状态
         * ========================================
         */
        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        player.getData(
                                QisPlan2.POSSESSED_GHOSTS
                        )
                );

        data.put(
                ghost,
                newState
        );

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );

        /*
         * ========================================
         * 复苏达到 100%
         *
         * 玩家死亡
         * ========================================
         */
        if (newState.revival() >= 1.0D) {

            player.setHealth(0.0F);

            return true;
        }

        /*
         * ========================================
         * 发动夜游鬼的灵异攻击
         * ========================================
         *
         * 夜游鬼驾驭后的攻击强度：
         * 0.4
         */
        SupernaturalDeathHandler.tryKill(
                target,
                ModDamageTypes.ghostNightWanderer(player),
                0.4D
        );

        return true;
    }


    /**
     * 夜游鬼驾驭效果：
     *
     * 暗处：
     *     移速大幅增加
     *
     * 亮处：
     *     移速降低
     *
     * 白天：
     *     失明
     *
     * 夜晚：
     *     夜视
     */
    private static void updateNightWandererEffects(
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

            // 白天失明
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

            // 确保没有夜视
            player.removeEffect(
                    MobEffects.NIGHT_VISION
            );

        } else {

            // 夜晚夜视
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

            // 确保没有失明
            player.removeEffect(
                    MobEffects.BLINDNESS
            );
        }

        /*
         * ========================================
         * 光照与移动速度
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
         * 夜游鬼自己的判定：
         *
         * 方块光 <= 3
         * 并且：
         *   夜晚
         *   或者天空光 <= 3
         *
         * → 暗处
         */
        boolean dark =
                blockLight <= 3
                        && (!day || skyLight <= 3);

        /*
         * 强方块光：
         * 火把、灯笼、萤石……
         *
         * 或者白天强天空光：
         * 露天太阳下
         */
        boolean bright =
                blockLight >= 8
                        || (day && skyLight >= 8);

        if (dark) {

            // 移除亮处减速
            removeNightWandererLightModifier(speed);

            // 添加暗处加速
            if (!speed.hasModifier(
                    NIGHT_WANDERER_DARK_SPEED
            )) {
                speed.addTransientModifier(
                        NIGHT_WANDERER_DARK_SPEED_MODIFIER
                );
            }

        } else if (bright) {

            // 移除暗处加速
            removeNightWandererDarkModifier(speed);

            // 添加亮处减速
            if (!speed.hasModifier(
                    NIGHT_WANDERER_LIGHT_SPEED
            )) {
                speed.addTransientModifier(
                        NIGHT_WANDERER_LIGHT_SPEED_MODIFIER
                );
            }

        } else {

            // 普通过渡亮度
            removeNightWandererDarkModifier(speed);
            removeNightWandererLightModifier(speed);
        }
    }

    private static final double DARK_SPEED =
            0.45D;

    private static final double LIGHT_SPEED =
            -0.10D;

    private static final ResourceLocation NIGHT_WANDERER_DARK_SPEED =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "possessed_night_wanderer_dark_speed"
            );

    private static final ResourceLocation NIGHT_WANDERER_LIGHT_SPEED =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "possessed_night_wanderer_light_speed"
            );

    private static final AttributeModifier NIGHT_WANDERER_DARK_SPEED_MODIFIER =
            new AttributeModifier(
                    NIGHT_WANDERER_DARK_SPEED,
                    DARK_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );

    private static final AttributeModifier NIGHT_WANDERER_LIGHT_SPEED_MODIFIER =
            new AttributeModifier(
                    NIGHT_WANDERER_LIGHT_SPEED,
                    LIGHT_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );

    private static void removeNightWandererDarkModifier(
            AttributeInstance attribute
    ) {
        if (attribute.hasModifier(
                NIGHT_WANDERER_DARK_SPEED
        )) {
            attribute.removeModifier(
                    NIGHT_WANDERER_DARK_SPEED
            );
        }
    }

    private static void removeNightWandererLightModifier(
            AttributeInstance attribute
    ) {
        if (attribute.hasModifier(
                NIGHT_WANDERER_LIGHT_SPEED
        )) {
            attribute.removeModifier(
                    NIGHT_WANDERER_LIGHT_SPEED
            );
        }
    }
}