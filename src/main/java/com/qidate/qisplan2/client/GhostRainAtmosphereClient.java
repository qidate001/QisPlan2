package com.qidate.qisplan2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class GhostRainAtmosphereClient {

    /**
     * 鬼雨天空暗淡程度。
     *
     * 0.0 = 不变
     * 1.0 = 非常暗
     */
    private static final double DARKNESS = 0.55D;

    private GhostRainAtmosphereClient() {
    }

    /**
     * 根据鬼雨领域修改天空颜色。
     */
    public static Vec3 applySkyDarkness(
            Vec3 original,
            double cameraX,
            double cameraZ
    ) {

        /*
         * 摄像机不在任何鬼雨领域。
         */
        if (!GhostUmbrellaDomainClient
                .isPositionInsideDomain(
                        cameraX,
                        cameraZ
                )) {

            return original;
        }

        double r =
                original.x;

        double g =
                original.y;

        double b =
                original.z;

        /*
         * ========================================================
         * 向灰色方向压缩
         * ========================================================
         *
         * 这一步是为了模拟原版雨天的感觉：
         *
         * 蓝天不会突然变黑，
         * 而是逐渐失去鲜艳程度，
         * 变成偏灰的阴天天空。
         */

        double gray =
                r * 0.30D
                        + g * 0.59D
                        + b * 0.11D;

        /*
         * 再稍微降低整体亮度。
         */
        gray *= 0.58D;

        /*
         * 混合。
         */
        r =
                r * (1.0D - DARKNESS)
                        + gray * DARKNESS;

        g =
                g * (1.0D - DARKNESS)
                        + gray * DARKNESS;

        b =
                b * (1.0D - DARKNESS)
                        + gray * DARKNESS;

        return new Vec3(
                r,
                g,
                b
        );
    }
}