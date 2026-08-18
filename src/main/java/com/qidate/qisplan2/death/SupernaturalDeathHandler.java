package com.qidate.qisplan2.death;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SupernaturalDeathHandler {

    /**
     * 尝试执行一次灵异死亡。
     *
     * @return true = 死亡成功
     *         false = 被抵消
     */
    public static boolean tryKill(
            LivingEntity entity,
            DamageSource damageSource
    ) {

        if (!entity.isAlive()) {
            return false;
        }


        /*
         * ========================================
         * 执行死亡
         * ========================================
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

        /*
         * 创造模式 + 未开启强制抹杀
         *
         * 直接抵消灵异攻击。
         */
        if (entity instanceof Player player
                && player.isCreative()
                && !instantlyKill) {

            return false;
        }

        /*
         * 开启强制抹杀
         */
        if (instantlyKill) {

            entity.setHealth(0.0F);

            return true;
        }

        return !entity.isAlive();
    }
}