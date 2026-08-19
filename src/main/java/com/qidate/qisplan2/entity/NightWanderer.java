package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public class NightWanderer extends PathfinderMob {

    /**
     * 灵异攻击后的休息时间：
     * 10 秒 = 200 tick
     */
    private static final int SUPERNATURAL_ATTACK_COOLDOWN = 200;

    /**
     * 当前灵异攻击冷却。
     */
    private int supernaturalAttackCooldown = 0;

    public NightWanderer(
            EntityType<? extends NightWanderer> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {

        /*
         * ========================================
         * 攻击 Goal
         * ========================================
         */

        this.goalSelector.addGoal(
                1,
                new SupernaturalAttackGoal(
                        this,
                        1.2D,
                        3.0D
                )
        );

        /*
         * ========================================
         * 目标选择
         * ========================================
         */

        // 玩家优先
        this.targetSelector.addGoal(
                1,
                new NearestAttackableTargetGoal<>(
                        this,
                        Player.class,
                        true
                )
        );

        // 没有玩家时，攻击其他 LivingEntity
        this.targetSelector.addGoal(
                2,
                new NearestAttackableTargetGoal<>(
                        this,
                        LivingEntity.class,
                        32,
                        true,
                        false,
                        target -> !(target instanceof Player)
                )
        );
    }

    /**
     * 夜游鬼每 tick 更新自身状态。
     */
    @Override
    public void aiStep() {
        super.aiStep();

        if (supernaturalAttackCooldown > 0) {
            supernaturalAttackCooldown--;
        }
    }

    /**
     * 夜游鬼无敌。
     */
    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return true;
    }

    /**
     * 实体属性。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(
                        Attributes.MAX_HEALTH,
                        20.0D
                )
                .add(
                        Attributes.MOVEMENT_SPEED,
                        0.25D
                )
                .add(
                        Attributes.FOLLOW_RANGE,
                        32.0D
                );
    }

    /**
     * 自定义灵异攻击 Goal。
     */
    private static class SupernaturalAttackGoal extends Goal {

        private final NightWanderer mob;
        private final double speedModifier;
        private final double attackRangeSqr;

        public SupernaturalAttackGoal(
                NightWanderer mob,
                double speedModifier,
                double attackRange
        ) {
            this.mob = mob;
            this.speedModifier = speedModifier;

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

            LivingEntity target =
                    mob.getTarget();

            return mob.supernaturalAttackCooldown <= 0
                    && target != null
                    && target.isAlive();
        }

        /**
         * 冷却开始或者目标消失时结束 Goal。
         */
        @Override
        public boolean canContinueToUse() {

            LivingEntity target =
                    mob.getTarget();

            return mob.supernaturalAttackCooldown <= 0
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

            SupernaturalDeathHandler.tryKill(
                    target,
                    ModDamageTypes.ghostNightWanderer(
                            target
                    )
            );

            /*
             * ========================================
             * 攻击结束，进入 10 秒休息
             * ========================================
             */

            mob.supernaturalAttackCooldown =
                    SUPERNATURAL_ATTACK_COOLDOWN;
        }
    }
}