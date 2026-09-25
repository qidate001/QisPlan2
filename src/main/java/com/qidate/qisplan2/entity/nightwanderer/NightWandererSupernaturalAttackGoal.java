package com.qidate.qisplan2.entity.nightwanderer;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * 夜游鬼的灵异攻击 Goal。
 *
 * <p>
 * 负责夜游鬼在拥有有效目标时的追踪、
 * 接近以及灵异攻击。
 *
 * <p>
 * 灵异攻击本身不会在这里决定夜游鬼的
 * 灵异强度、击杀记录等数据，
 * 这些数据仍然由 {@link NightWanderer}
 * 实体本体提供。
 */
public class NightWandererSupernaturalAttackGoal
        extends Goal {

    private final NightWanderer mob;

    private final double speedModifier;

    private final double attackRangeSqr;

    public NightWandererSupernaturalAttackGoal(
            NightWanderer mob,
            double speedModifier,
            double attackRange
    ) {
        this.mob = mob;

        this.speedModifier =
                speedModifier;

        this.attackRangeSqr =
                attackRange * attackRange;

        this.setFlags(
                EnumSet.of(
                        Goal.Flag.MOVE,
                        Goal.Flag.LOOK
                )
        );
    }

    /**
     * 冷却结束并且存在有效目标时开始 Goal。
     */
    @Override
    public boolean canUse() {

        /*
         * 白天禁止移动。
         */
        if (mob.level().isDay()) {
            return false;
        }

        LivingEntity target =
                mob.getTarget();

        return !mob.isSupernaturallyStunned()
                && mob.isSupernaturalAttackReady()
                && target != null
                && target.isAlive();
    }

    /**
     * 冷却开始或者目标消失时结束 Goal。
     */
    @Override
    public boolean canContinueToUse() {

        /*
         * 白天禁止移动。
         */
        if (mob.level().isDay()) {
            return false;
        }

        LivingEntity target =
                mob.getTarget();

        return !mob.isSupernaturallyStunned()
                && mob.isSupernaturalAttackReady()
                && target != null
                && target.isAlive();
    }

    @Override
    public void start() {

        LivingEntity target =
                mob.getTarget();

        if (target != null) {

            mob.getNavigation().moveTo(
                    target,
                    speedModifier
            );
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {

        LivingEntity target =
                mob.getTarget();

        if (target == null
                || !target.isAlive()) {

            return;
        }

        /*
         * 一直看向目标。
         */
        mob.getLookControl().setLookAt(
                target,
                30.0F,
                30.0F
        );

        double distanceSqr =
                mob.distanceToSqr(target);

        /*
         * ========================================
         * 还没进入攻击距离
         * ========================================
         */
        if (distanceSqr > attackRangeSqr) {

            mob.getNavigation().moveTo(
                    target,
                    speedModifier
            );

            return;
        }

        /*
         * ========================================
         * 灵异攻击
         * ========================================
         */

        mob.getNavigation().stop();

        mob.swing(
                InteractionHand.MAIN_HAND
        );

        boolean killed =
                SupernaturalDeathHandler.tryKill(
                        target,
                        ModDamageTypes.ghostNightWanderer(mob),
                        mob.getSupernaturalStrength()
                );

        if (killed) {
            mob.onKillEntity();
        }

        /*
         * ========================================
         * 攻击结束，进入休息时间
         * ========================================
         */
        mob.startSupernaturalAttackCooldown();
    }
}