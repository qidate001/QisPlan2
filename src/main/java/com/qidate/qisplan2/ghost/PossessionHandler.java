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
     * 复苏速度：
     *
     * 每 10 秒恢复 1%。
     *
     * 100% 大约需要 16 分 40 秒。
     */
    public static final double REVIVAL_RECOVERY_PER_TICK =
            0.01D / 200.0D;


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


    /**
     * 每 tick 更新所有鬼的复苏值。
     */
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

        for (var entry : oldData.entrySet()) {

            ResourceLocation ghost =
                    entry.getKey();

            PossessedGhostState state =
                    entry.getValue();

            double revival =
                    state.revival();

            /*
             * 鬼逐渐复苏。
             */
            revival +=
                    REVIVAL_RECOVERY_PER_TICK;

            revival =
                    Math.min(
                            1.0D,
                            revival
                    );

            if (revival != state.revival()) {

                changed = true;

                data.put(
                        ghost,
                        new PossessedGhostState(
                                revival,
                                state.lastAbilityUseTick()
                        )
                );
            }

            /*
             * 达到 100%。
             */
            if (revival >= 1.0D) {

                player.setHealth(0.0F);

                return;
            }
        }

        if (changed) {
            player.setData(
                    QisPlan2.POSSESSED_GHOSTS,
                    data
            );
        }

        /*
         * 夜游鬼目前的能力
         * 继续调用你之前的效果处理。
         */
        updateNightWandererEffects(player);
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

        ResourceLocation ghost =
                NIGHT_WANDERER;

        PossessedGhostState state =
                getState(player, ghost);

        if (state == null) {
            return false;
        }

        long now =
                player.serverLevel()
                        .getGameTime();

        /*
         * 判断是不是 10 秒内再次使用。
         */
        boolean rapid =
                now - state.lastAbilityUseTick()
                        < TEN_SECONDS;

        double revivalGain =
                rapid
                        ? RAPID_REVIVAL_GAIN
                        : NORMAL_REVIVAL_GAIN;

        double revival =
                Math.min(
                        1.0D,
                        state.revival()
                                + revivalGain
                );

        /*
         * 更新复苏值和时间。
         */
        Map<ResourceLocation, PossessedGhostState> data =
                new HashMap<>(
                        player.getData(
                                QisPlan2.POSSESSED_GHOSTS
                        )
                );

        data.put(
                ghost,
                new PossessedGhostState(
                        revival,
                        now
                )
        );

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                data
        );

        /*
         * ========================================
         * 100% → 玩家死亡
         * ========================================
         */

        if (revival >= 1.0D) {
            player.setHealth(0.0F);
            return true;
        }

        /*
         * ========================================
         * 夜游鬼的灵异攻击
         * ========================================
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