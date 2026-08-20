package com.qidate.qisplan2.death;

import com.qidate.qisplan2.item.DeathCurseSword;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class SupernaturalCombatHandler {

    private SupernaturalCombatHandler() {
    }

    /**
     * 判断一个灵异实体是否允许受到普通伤害。
     *
     * false = 完全免疫
     * true  = 可以正常进入伤害流程
     */
    public static boolean canReceiveNormalDamage(
            LivingEntity entity,
            DamageSource damageSource
    ) {

        /*
         * ========================================
         * 死亡诅咒之剑
         * ========================================
         *
         * 允许它进入正常伤害事件。
         *
         * DeathCurseHandler 会把实际伤害改成 0，
         * 但仍然可以叠加诅咒层数。
         */
        if (damageSource.getEntity() instanceof Player player) {

            if (player.getMainHandItem().getItem()
                    instanceof DeathCurseSword) {

                return true;
            }
        }

        /*
         * 以后其他特殊武器、特殊攻击都在这里加入。
         */

        return false;
    }


    /**
     * 统一判断是否应当免疫普通伤害。
     */
    public static boolean isInvulnerableTo(
            LivingEntity entity,
            DamageSource damageSource
    ) {
        return !canReceiveNormalDamage(
                entity,
                damageSource
        );
    }
}