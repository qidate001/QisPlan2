package com.qidate.qisplan2.client;

import com.qidate.qisplan2.client.domain.ClientGhostDomain;
import com.qidate.qisplan2.client.domain.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.ability.ghosteye.GhostEyeAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class GhostDomainAtmosphereClient {

    /**
     * 鬼雨天空暗淡程度。
     *
     * 0.0 = 不变
     * 1.0 = 非常暗
     */
    private static final double RAIN_DARKNESS = 0.55D;

    private GhostDomainAtmosphereClient() {
    }

    /**
     * 根据当前有效鬼域修改天空颜色。
     *
     * 这是所有鬼域天空视觉效果的统一入口。
     */
    public static Vec3 applySkyColor(
            Vec3 original,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {

        /*
         * 找到摄像机当前位置的有效鬼域。
         *
         * 优先级：
         * 1. layer 更高
         * 2. layer 相同时 strength 更高
         */
        ClientGhostDomain domain =
                ClientGhostDomainManager.getEffectiveDomain(
                        cameraX,
                        cameraY,
                        cameraZ
                );

        if (domain == null) {
            return original;
        }

        /*
         * ========================================================
         * 根据鬼域类型处理视觉效果
         * ========================================================
         */

        // 鬼雨
        if (isGhostRainDomain(domain)) {
            return applyGhostRainSkyColor(original);
        }

        // 鬼眼
        if (GhostEyeAbility.ID.equals(domain.getDomainType())) {
            return applyGhostEyeSkyColor(
                    original,
                    domain
            );
        }

        /*
         * 其他暂时没有天空效果的鬼域。
         */
        return original;
    }

    // ============================================================
    // 鬼雨
    // ============================================================

    /**
     * 鬼雨天空效果。
     *
     * 根据原来的 GhostRainAtmosphereClient
     * 完整迁移而来。
     */
    private static Vec3 applyGhostRainSkyColor(
            Vec3 original
    ) {

        double r = original.x;
        double g = original.y;
        double b = original.z;

        /*
         * ========================================================
         * 向灰色方向压缩
         * ========================================================
         *
         * 模拟原版雨天的天空：
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
                r * (1.0D - RAIN_DARKNESS)
                        + gray * RAIN_DARKNESS;

        g =
                g * (1.0D - RAIN_DARKNESS)
                        + gray * RAIN_DARKNESS;

        b =
                b * (1.0D - RAIN_DARKNESS)
                        + gray * RAIN_DARKNESS;

        return new Vec3(
                r,
                g,
                b
        );
    }

    /**
     * 判断一个鬼域是否为鬼雨领域。
     *
     * TODO：
     * 如果你的鬼雨领域已经有固定的 ResourceLocation，
     * 这里替换成对应的 ID。
     */
    private static boolean isGhostRainDomain(
            ClientGhostDomain domain
    ) {

        return domain.getDomainType().equals(
                ResourceLocation.fromNamespaceAndPath(
                        "qisplan2",
                        "ghost_umbrella"
                )
        );
    }

    // ============================================================
    // 鬼眼
    // ============================================================

    /**
     * 鬼眼天空效果。
     */
    private static Vec3 applyGhostEyeSkyColor(
            Vec3 original,
            ClientGhostDomain domain
    ) {

        if (domain.getLayer() != 1) {
            return original;
        }

        // 测试阶段：直接使用纯红色
        return new Vec3(
                1.0D,
                0.0D,
                0.0D
        );
    }
}