package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LightLayer;

import java.util.ArrayList;
import java.util.List;

public final class PossessionHandler {

    private PossessionHandler() {
    }

    /*
     * ========================================
     * 夜游鬼 ID
     * ========================================
     */

    public static final ResourceLocation NIGHT_WANDERER =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "night_wanderer"
            );


    /*
     * ========================================
     * 夜游鬼驾驭效果
     * ========================================
     */

    private static final double DARK_SPEED =
            0.45D;

    private static final double LIGHT_SPEED =
            -0.10D;

    private static final ResourceLocation
            NIGHT_WANDERER_DARK_SPEED =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "possessed_night_wanderer_dark_speed"
            );

    private static final ResourceLocation
            NIGHT_WANDERER_LIGHT_SPEED =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "possessed_night_wanderer_light_speed"
            );

    private static final AttributeModifier
            NIGHT_WANDERER_DARK_SPEED_MODIFIER =
            new AttributeModifier(
                    NIGHT_WANDERER_DARK_SPEED,
                    DARK_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );

    private static final AttributeModifier
            NIGHT_WANDERER_LIGHT_SPEED_MODIFIER =
            new AttributeModifier(
                    NIGHT_WANDERER_LIGHT_SPEED,
                    LIGHT_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );


    /**
     * 判断玩家是否驾驭了指定鬼。
     */
    public static boolean hasGhost(
            ServerPlayer player,
            ResourceLocation ghost
    ) {
        return player.getData(
                QisPlan2.POSSESSED_GHOSTS
        ).contains(ghost);
    }


    /**
     * 驾驭一只鬼。
     *
     * @return true = 本次成功新增
     */
    public static boolean possess(
            ServerPlayer player,
            ResourceLocation ghost
    ) {
        List<ResourceLocation> ghosts =
                new ArrayList<>(
                        player.getData(
                                QisPlan2.POSSESSED_GHOSTS
                        )
                );

        if (ghosts.contains(ghost)) {
            return false;
        }

        ghosts.add(ghost);

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                ghosts
        );

        return true;
    }


    /**
     * 解除驾驭。
     *
     * @return true = 原本确实驾驭了
     */
    public static boolean release(
            ServerPlayer player,
            ResourceLocation ghost
    ) {
        List<ResourceLocation> ghosts =
                new ArrayList<>(
                        player.getData(
                                QisPlan2.POSSESSED_GHOSTS
                        )
                );

        if (!ghosts.remove(ghost)) {
            return false;
        }

        player.setData(
                QisPlan2.POSSESSED_GHOSTS,
                ghosts
        );

        return true;
    }


    /**
     * 每 tick 更新驾驭效果。
     */
    public static void tick(
            ServerPlayer player
    ) {

        boolean possessesNightWanderer =
                hasGhost(
                        player,
                        NIGHT_WANDERER
                );

        if (possessesNightWanderer) {
            updateNightWandererEffect(player);
        } else {
            removeNightWandererEffect(player);
        }
    }


    /**
     * 夜游鬼能力：
     *
     * 暗处：
     *     移速增加
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
    private static void updateNightWandererEffect(
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
         * 白天
         * ========================================
         */

        if (day) {

            /*
             * 失明
             */
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

            /*
             * 白天不提供夜视。
             */
            player.removeEffect(
                    MobEffects.NIGHT_VISION
            );

        } else {

            /*
             * 夜晚夜视
             */
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

            /*
             * 夜晚不失明。
             */
            player.removeEffect(
                    MobEffects.BLINDNESS
            );
        }


        /*
         * ========================================
         * 根据光照调整移动速度
         * ========================================
         */

        AttributeInstance speed =
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (speed == null) {
            return;
        }

        boolean dark =
                blockLight <= 3
                        && (!day || skyLight <= 3);

        boolean bright =
                blockLight >= 8
                        || (day && skyLight >= 8);

        if (dark) {

            removeLightModifier(speed);

            if (!speed.hasModifier(
                    NIGHT_WANDERER_DARK_SPEED
            )) {
                speed.addTransientModifier(
                        NIGHT_WANDERER_DARK_SPEED_MODIFIER
                );
            }

        } else if (bright) {

            removeDarkModifier(speed);

            if (!speed.hasModifier(
                    NIGHT_WANDERER_LIGHT_SPEED
            )) {
                speed.addTransientModifier(
                        NIGHT_WANDERER_LIGHT_SPEED_MODIFIER
                );
            }

        } else {

            removeDarkModifier(speed);
            removeLightModifier(speed);
        }
    }


    private static void removeDarkModifier(
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


    private static void removeLightModifier(
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


    /**
     * 没有驾驭夜游鬼时清理其效果。
     */
    private static void removeNightWandererEffect(
            ServerPlayer player
    ) {

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

        if (speed != null) {
            removeDarkModifier(speed);
            removeLightModifier(speed);
        }
    }
}