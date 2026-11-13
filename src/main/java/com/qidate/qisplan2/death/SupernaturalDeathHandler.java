package com.qidate.qisplan2.death;

import com.qidate.qisplan2.event.GhostEntityDestructionHandler;
import com.qidate.qisplan2.ghost.module.host.GhostAttackBridge;
import com.qidate.qisplan2.core.ModGameRules;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurse;
import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import com.qidate.qisplan2.ghost.module.implementation.divinationslip.GhostDivinationSlipSystem;
import com.qidate.qisplan2.ghost.GhostAvoidanceSystem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

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
     * 尝试执行一次灵异袭击。
     *
     * @param entity 目标
     * @param damageSource 灵异攻击伤害来源
     * @param supernaturalIntensity 灵异攻击强度
     *
     * @return true = 死亡成功
     *         false = 被抵消/抵挡
     */
    public static boolean tryKill(
            LivingEntity entity,
            DamageSource damageSource,
            double supernaturalIntensity
    ) {
//        QisPlan2.LOGGER.info(
//                "[QisPlan2] tryKill 被调用：目标={}，强度={}",
//                entity.getName().getString(),
//                supernaturalIntensity
//        );


        if (!entity.isAlive()) {
            return false;
        }

        /*
         * ========================================
         * 鬼层隔离
         * ========================================
         *
         * 不同灵异层之间无法互相攻击。
         * 现实层（0）与任何层级之间都可以攻击。
         */
        if (damageSource.getEntity() != null
                && !GhostLayerHandler.canSee(
                damageSource.getEntity(),
                entity
        )) {

            return false;
        }

        // 非有限强度不能参与抵挡和后续生命代价计算。
        if (!Double.isFinite(supernaturalIntensity)) return false;
        supernaturalIntensity =
                Math.max(0.0D, supernaturalIntensity);

        // 手持燃烧白烛令袭击停止，不再采用旧版“挨打后扣耐久抵消强度”。
        // 延迟袭击也在这里读取最新状态，点燃后立即有效，熄灭或燃尽后立即失效。
        if (GhostAvoidanceSystem.hasWhiteGhostCandle(entity)) return false;

        // 模块先处理完整或部分抵挡，后续保护与停滞计算只接收剩余强度。
        var attack = GhostAttackBridge.resolve(entity, supernaturalIntensity);
        if (attack.isBlocked()) return false;
        supernaturalIntensity = attack.remainingIntensity();

        // 生签统计真正穿过前层防护的强度，不重新计入已被寿衣抵挡的部分。
        double recentIntensity =
                SupernaturalIntensityTracker
                        .addAndGetRecentIntensity(
                                entity,
                                supernaturalIntensity
                        );

        /*
         * 生签：
         *
         * 最近一秒累计灵异强度
         * V级时，小于 50 时抵挡。
         *
         * 达到或超过 50，
         * 本次袭击击穿。
         */
        GhostDivinationCurse lifeCurse =
                GhostDivinationSlipSystem.getLifeCurse(
                        entity.getUUID()
                );

        if (lifeCurse != null) {

            double protectionStrength =
                    lifeCurse.getStrength();

            if (recentIntensity < protectionStrength) {
                return false;
            }
        }

        /*
         * 灵异实体受到灵异攻击
         */
        if (entity instanceof SupernaturalEntity supernaturalEntity) {
//            QisPlan2.LOGGER.info(
//                    "[QisPlan2] 灵异攻击命中灵异实体：{}",
//                    entity.getName().getString()
//            );
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
        boolean instantlyKill = entity.level().getGameRules()
                .getRule(ModGameRules.GHOST_DAMAGE_INSTANTLY_KILL).get();

        if (!instantlyKill) {
            // 非强制模式交给真实死亡事件；受伤被免疫或抵挡时，不提前触发摧毁反击。
            entity.hurt(damageSource, Float.MAX_VALUE);
            return !entity.isAlive();
        }

        // 强制模式本来会绕过原版死亡取消，必须在伤害及强制归零前统一检查。
        return GhostEntityDestructionHandler.executeLethalAttack(entity, damageSource, () -> {
            entity.hurt(damageSource, Float.MAX_VALUE);
            QisDeathHandler.forceKillAndCleanup(entity);
            return !entity.isAlive();
        });
    }

}
