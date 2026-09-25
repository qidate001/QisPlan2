package com.qidate.qisplan2.entity.nightwanderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LightLayer;

/**
 * 负责夜游鬼根据环境光照调整移动速度。
 */
public final class NightWandererMovementSystem {

    private static final double NORMAL_SPEED = 0.25D;
    private static final double DARK_SPEED = 0.8D;
    private static final double LIGHT_SPEED = 0.12D;

    private static final ResourceLocation DARK_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "night_wanderer_dark_speed"
            );

    private static final ResourceLocation LIGHT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "night_wanderer_light_speed"
            );

    private static final AttributeModifier DARK_SPEED_MODIFIER =
            new AttributeModifier(
                    DARK_SPEED_MODIFIER_ID,
                    DARK_SPEED - NORMAL_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );

    private static final AttributeModifier LIGHT_SPEED_MODIFIER =
            new AttributeModifier(
                    LIGHT_SPEED_MODIFIER_ID,
                    LIGHT_SPEED - NORMAL_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );

    private NightWandererMovementSystem() {
    }

    /**
     * 更新夜游鬼的光照移动速度。
     *
     * @param ghost 夜游鬼
     */
    public static void tick(
            NightWanderer ghost
    ) {

        if (ghost.level().isClientSide()) {
            return;
        }

        AttributeInstance speedAttribute =
                ghost.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (speedAttribute == null) {
            return;
        }

        int blockLight =
                ghost.level().getBrightness(
                        LightLayer.BLOCK,
                        ghost.blockPosition()
                );

        int skyLight =
                ghost.level().getBrightness(
                        LightLayer.SKY,
                        ghost.blockPosition()
                );

        boolean isDay =
                ghost.level().isDay();

        /*
         * ========================================
         * 亮处
         * ========================================
         *
         * 1. 方块光很强：火把、灯笼、萤石等
         * 2. 白天天空光很强：露天环境
         */
        boolean bright =
                blockLight >= 8
                        || (isDay && skyLight >= 8);

        /*
         * ========================================
         * 暗处
         * ========================================
         *
         * 1. 方块光很低
         * 2. 夜晚不考虑天空光本身
         *
         * 因此夜晚露天也可以进入高速状态。
         */
        boolean dark =
                blockLight <= 3
                        && (!isDay || skyLight <= 3);

        if (dark) {

            removeLightSpeedModifier(
                    speedAttribute
            );

            if (!speedAttribute.hasModifier(
                    DARK_SPEED_MODIFIER_ID
            )) {
                speedAttribute.addTransientModifier(
                        DARK_SPEED_MODIFIER
                );
            }

        } else if (bright) {

            removeDarkSpeedModifier(
                    speedAttribute
            );

            if (!speedAttribute.hasModifier(
                    LIGHT_SPEED_MODIFIER_ID
            )) {
                speedAttribute.addTransientModifier(
                        LIGHT_SPEED_MODIFIER
                );
            }

        } else {

            removeDarkSpeedModifier(
                    speedAttribute
            );

            removeLightSpeedModifier(
                    speedAttribute
            );
        }
    }

    private static void removeDarkSpeedModifier(
            AttributeInstance attribute
    ) {
        if (attribute.hasModifier(
                DARK_SPEED_MODIFIER_ID
        )) {
            attribute.removeModifier(
                    DARK_SPEED_MODIFIER_ID
            );
        }
    }

    private static void removeLightSpeedModifier(
            AttributeInstance attribute
    ) {
        if (attribute.hasModifier(
                LIGHT_SPEED_MODIFIER_ID
        )) {
            attribute.removeModifier(
                    LIGHT_SPEED_MODIFIER_ID
            );
        }
    }
}