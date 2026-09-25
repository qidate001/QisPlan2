package com.qidate.qisplan2.item.candle;

import com.qidate.qisplan2.entity.AbstractGhostEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 管理鬼烛与厉鬼之间的燃烧关系。
 *
 * <p>
 * 厉鬼越接近鬼烛持有者，
 * 以及厉鬼的灵异强度越高，
 * 鬼烛燃烧得越快。
 *
 * <p>
 * 本系统负责：
 *
 * <ul>
 *     <li>寻找附近的厉鬼</li>
 *     <li>计算厉鬼造成的燃烧压力</li>
 *     <li>将小数燃烧损耗转换为整数耐久损耗</li>
 * </ul>
 *
 * <p>
 * 本系统不负责判断鬼烛是否点燃，
 * 也不负责修改鬼烛的点燃模型。
 */
public final class GhostCandleSystem {

    private GhostCandleSystem() {
    }

    /**
     * 距离影响开始生效的范围。
     *
     * <p>
     * 超过该距离时，
     * 厉鬼不会因为距离额外加速鬼烛燃烧。
     */
    private static final double DISTANCE_EFFECT_RANGE =
            16.0D;

    /**
     * 距离造成的最大倍率。
     *
     * <p>
     * 16 格以外为 1 倍，
     * 贴近厉鬼时最高为 5 倍。
     */
    private static final double MAX_DISTANCE_MULTIPLIER =
            5.0D;

    /**
     * 每 tick 的基础燃烧损耗。
     *
     * <p>
     * 正常情况下：
     *
     * 20 tick × 0.05 = 1 点耐久。
     */
    private static final double BASE_BURN_DAMAGE_PER_TICK =
            1.0D / 20.0D;

    /**
     * 计算指定玩家附近最大的鬼烛燃烧压力。
     *
     * <p>
     * 当附近存在多只厉鬼时，
     * 取其中燃烧压力最高的一只。
     *
     * <p>
     * 这样可以避免多只厉鬼简单叠加，
     * 导致鬼烛燃烧速度随着厉鬼数量无限增加。
     *
     * @param player 鬼烛持有者
     * @return 每 tick 的耐久损耗
     */
    public static double getBurnDamagePerTick(
            Player player
    ) {
        double searchRange =
                DISTANCE_EFFECT_RANGE;

        AbstractGhostEntity strongestGhost =
                null;

        double strongestBurnDamage =
                BASE_BURN_DAMAGE_PER_TICK;

        for (AbstractGhostEntity ghost
                : player.level().getEntitiesOfClass(
                AbstractGhostEntity.class,
                player.getBoundingBox().inflate(
                        searchRange
                ),
                ghost ->
                        ghost.isAlive()
                                && !ghost.isRemoved()
        )) {

            double burnDamage =
                    getBurnDamagePerTick(
                            player,
                            ghost
                    );

            if (burnDamage > strongestBurnDamage) {
                strongestBurnDamage =
                        burnDamage;

                strongestGhost =
                        ghost;
            }
        }

        return strongestBurnDamage;
    }

    /**
     * 计算指定厉鬼对鬼烛造成的每 tick 耐久损耗。
     *
     * @param player 鬼烛持有者
     * @param ghost 厉鬼
     * @return 每 tick 的耐久损耗
     */
    public static double getBurnDamagePerTick(
            Player player,
            AbstractGhostEntity ghost
    ) {
        double distance =
                player.distanceTo(ghost);

        double distanceMultiplier =
                getDistanceMultiplier(
                        distance
                );

        double strengthMultiplier =
                getStrengthMultiplier(
                        ghost.getSupernaturalStrength()
                );

        return BASE_BURN_DAMAGE_PER_TICK
                * distanceMultiplier
                * strengthMultiplier;
    }

    /**
     * 根据距离计算燃烧倍率。
     *
     * <p>
     * 距离越近，倍率越高。
     */
    private static double getDistanceMultiplier(
            double distance
    ) {
        if (distance >= DISTANCE_EFFECT_RANGE) {
            return 1.0D;
        }

        double normalized =
                1.0D
                        - distance
                        / DISTANCE_EFFECT_RANGE;

        return 1.0D
                + normalized
                * (MAX_DISTANCE_MULTIPLIER - 1.0D);
    }

    /**
     * 根据厉鬼灵异强度计算燃烧倍率。
     *
     * <p>
     * 每增加 10 点灵异强度，
     * 燃烧倍率增加 1 倍。
     */
    private static double getStrengthMultiplier(
            double strength
    ) {
        return 1.0D
                + Math.max(
                0.0D,
                strength
        ) / 10.0D;
    }

    /**
     * 将小数形式的燃烧损耗转换为本 tick
     * 实际应该扣除的整数耐久。
     *
     * <p>
     * 例如：
     *
     * <ul>
     *     <li>0.05 → 5% 概率扣 1</li>
     *     <li>0.75 → 75% 概率扣 1</li>
     *     <li>2.75 → 必定扣 2，75% 概率额外扣 1</li>
     *     <li>10.3 → 必定扣 10，30% 概率额外扣 1</li>
     * </ul>
     *
     * @param player 鬼烛持有者
     * @param burnDamage 每 tick 燃烧损耗
     * @return 本 tick 实际扣除的耐久
     */
    public static int rollBurnDamage(
            Player player,
            double burnDamage
    ) {
        int guaranteedDamage =
                (int) Math.floor(
                        burnDamage
                );

        double fractionalDamage =
                burnDamage
                        - guaranteedDamage;

        if (fractionalDamage > 0.0D
                && player.getRandom()
                .nextDouble()
                < fractionalDamage) {

            guaranteedDamage++;
        }

        return Math.max(
                0,
                guaranteedDamage
        );
    }
}