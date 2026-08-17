package com.qidate.qisplan2.death;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

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
         * 以后所有“死亡抵消”机制都放这里
         * ========================================
         *
         * 例如：
         *
         * if (hasDeathProtection(entity)) {
         *     consumeDeathProtection(entity);
         *     return false;
         * }
         */


        /*
         * ========================================
         * 执行死亡
         * ========================================
         *
         * 使用 DamageSource，而不是 entity.kill()
         *
         * 这样 Minecraft 才能知道：
         *
         * “这个人是被 ghost_carpet 杀死的”
         *
         * 从而自动使用：
         *
         * death.attack.ghost_carpet
         */
        entity.hurt(
                damageSource,
                Float.MAX_VALUE
        );

        entity.setHealth(0.0F);

        return !entity.isAlive();
    }
}