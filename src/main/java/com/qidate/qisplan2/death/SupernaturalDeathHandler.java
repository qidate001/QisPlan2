package com.qidate.qisplan2.death;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.SupernaturalEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SupernaturalDeathHandler {

    /**
     * 基础灵异停滞时间：
     *
     * 100 tick = 5 秒
     *
     * 攻击强度 1 + 防御 1 时使用这个时间。
     */
    private static final int BASE_STUN_TIME = 100;

    /**
     * 最低停滞时间：
     *
     * 5 tick = 0.25 秒
     */
    private static final int MIN_STUN_TIME = 5;


    /**
     * 普通灵异攻击。
     *
     * 默认灵异强度 = 1。
     */
    public static boolean tryKill(
            LivingEntity entity,
            DamageSource damageSource
    ) {
        return tryKill(
                entity,
                damageSource,
                1.0D
        );
    }


    /**
     * 尝试执行一次灵异死亡。
     *
     * @param entity 目标
     * @param damageSource 灵异攻击伤害来源
     * @param supernaturalIntensity 灵异攻击强度
     *
     * @return true = 死亡成功
     *         false = 被抵消
     */
    public static boolean tryKill(
            LivingEntity entity,
            DamageSource damageSource,
            double supernaturalIntensity
    ) {

        if (!entity.isAlive()) {
            return false;
        }

        supernaturalIntensity =
                Math.max(0.0D, supernaturalIntensity);

        /*
         * 灵异实体受到灵异攻击
         */
        if (entity instanceof SupernaturalEntity supernaturalEntity) {

            double defense =
                    Math.max(
                            0.01D,
                            supernaturalEntity.getSupernaturalDefense()
                    );

            double stunTicksDouble =
                    BASE_STUN_TIME
                            * supernaturalIntensity
                            / defense;

            int stunTicks =
                    Math.max(
                            MIN_STUN_TIME,
                            (int) Math.round(stunTicksDouble)
                    );

            supernaturalEntity.onSupernaturalAttack(
                    stunTicks
            );

            return false;
        }

        /*
         * 普通实体仍然是秒杀逻辑
         */
        entity.hurt(
                damageSource,
                Float.MAX_VALUE
        );

        boolean instantlyKill =
                entity.level()
                        .getGameRules()
                        .getRule(
                                QisPlan2.GHOST_DAMAGE_INSTANTLY_KILL
                        )
                        .get();

        if (entity instanceof Player player
                && player.isCreative()
                && !instantlyKill) {

            return false;
        }

        if (instantlyKill) {
            entity.setHealth(0.0F);
            return true;
        }

        return !entity.isAlive();
    }
}