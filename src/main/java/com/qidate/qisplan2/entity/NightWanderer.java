package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalCombatHandler;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.entity.ai.GhostWanderGoal;
import com.qidate.qisplan2.item.DeathCurseSword;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.util.EnumSet;

public class NightWanderer
        extends AbstractGhostEntity
{

    private static final double NORMAL_SPEED = 0.25D;
    private static final double DARK_SPEED = 0.8D;
    private static final double LIGHT_SPEED = 0.12D;

    private static final ResourceLocation DARK_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "night_wanderer_dark_speed"
            );

    private static final ResourceLocation LIGHT_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "night_wanderer_light_speed"
            );

    private static final AttributeModifier DARK_SPEED_MODIFIER =
            new AttributeModifier(
                    DARK_SPEED_MODIFIER_ID,
                    DARK_SPEED - NORMAL_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );

    private static final AttributeModifier LIGHT_SPEED_MODIFIER =
            new AttributeModifier(
                    LIGHT_SPEED_MODIFIER_ID,
                    LIGHT_SPEED - NORMAL_SPEED,
                    AttributeModifier.Operation.ADD_VALUE
            );

    /**
     * 灵异攻击后的死机时间。
     *
     * 10 秒 = 200 tick
     */
    private static final int SUPERNATURAL_STUN_TIME = 100;

    /**
     * 灵异攻击后的休息时间：
     * 10 秒 = 200 tick
     */
    private static final int SUPERNATURAL_ATTACK_COOLDOWN = 200;

    /**
     * 当前灵异攻击冷却。
     */
    private int supernaturalAttackCooldown = 0;

    /**
     * 灵异攻击强度
     */
    private static final double SUPERNATURAL_ATTACK_STRENGTH = 4.0D;
    private static final double SUPERNATURAL_DEFENSE = 6.0D;

    @Override
    public double getSupernaturalDefense() {
        return SUPERNATURAL_DEFENSE;
    }



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
         * 玩家优先
         * ========================================
         */
//        this.targetSelector.addGoal(
//                1,
//                new NearestAttackableTargetGoal<>(
//                        this,
//                        Player.class,
//                        32,
//                        true,
//                        false,
//                        target -> target instanceof Player
//                )
//        );

        /*
         * ========================================
         * 没有玩家时，攻击其他 LivingEntity
         * ========================================
         */
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

        /*
         * ========================================
         * 没有目标，四处游荡
         * ========================================
         */
        this.goalSelector.addGoal(
                8,
                new GhostWanderGoal(
                        this,
                        0.7D
                )
        );
    }

    /**
     * 夜游鬼每 tick 更新自身状态。
     */
    @Override
    public void aiStep() {

        super.aiStep();

        /*
         * ========================================
         * 死机状态
         * ========================================
         *
         * AbstractGhostEntity 已经处理。
         *
         * 如果当前处于死机，
         * 不继续执行夜游鬼自己的行为。
         */
        if (isSupernaturallyStunned()) {
            return;
        }

        /*
         * ========================================
         * 玩家优先
         * ========================================
         */

        if (!level().isClientSide()) {

            Player player =
                    level().getNearestPlayer(
                            this,
                            32.0D
                    );

            if (player != null
                    && player.isAlive()
                    && !player.isSpectator()
                    && !player.isCreative()) {

                setTarget(player);
            }
        }

        /*
         * ========================================
         * 自身攻击冷却
         * ========================================
         */

        if (supernaturalAttackCooldown > 0) {
            supernaturalAttackCooldown--;
        }

        /*
         * ========================================
         * 光照移速
         * ========================================
         */

        updateMovementSpeed();
    }

    private void updateMovementSpeed() {

        if (level().isClientSide()) {
            return;
        }

        AttributeInstance speedAttribute =
                getAttribute(Attributes.MOVEMENT_SPEED);

        if (speedAttribute == null) {
            return;
        }

        int blockLight =
                level().getBrightness(
                        LightLayer.BLOCK,
                        blockPosition()
                );

        int skyLight =
                level().getBrightness(
                        LightLayer.SKY,
                        blockPosition()
                );

        boolean isDay =
                level().isDay();

        /*
         * ========================================
         * 亮处
         * ========================================
         *
         * 1. 方块光很强：火把、灯笼、萤石等
         * 2. 白天天空光很强：露天环境
         */
        boolean bright =
                blockLight >= 8
                        || (isDay && skyLight >= 8);

        /*
         * ========================================
         * 暗处
         * ========================================
         *
         * 1. 方块光很低
         * 2. 夜晚不考虑天空光本身
         *
         * 因此夜晚露天也可以进入高速状态。
         */
        boolean dark =
                blockLight <= 3
                        && (!isDay || skyLight <= 3);

        if (dark) {

            // 移除减速
            removeLightSpeedModifier(speedAttribute);

            // 添加高速
            if (!speedAttribute.hasModifier(
                    DARK_SPEED_MODIFIER_ID
            )) {
                speedAttribute.addTransientModifier(
                        DARK_SPEED_MODIFIER
                );
            }

        } else if (bright) {

            // 移除高速
            removeDarkSpeedModifier(speedAttribute);

            // 添加减速
            if (!speedAttribute.hasModifier(
                    LIGHT_SPEED_MODIFIER_ID
            )) {
                speedAttribute.addTransientModifier(
                        LIGHT_SPEED_MODIFIER
                );
            }

        } else {

            // 普通环境
            removeDarkSpeedModifier(speedAttribute);
            removeLightSpeedModifier(speedAttribute);
        }
    }

    private void removeDarkSpeedModifier(
            AttributeInstance attribute
    ) {
        if (attribute.hasModifier(DARK_SPEED_MODIFIER_ID)) {
            attribute.removeModifier(DARK_SPEED_MODIFIER_ID);
        }
    }

    private void removeLightSpeedModifier(
            AttributeInstance attribute
    ) {
        if (attribute.hasModifier(LIGHT_SPEED_MODIFIER_ID)) {
            attribute.removeModifier(LIGHT_SPEED_MODIFIER_ID);
        }
    }

    /**
     * 夜游鬼无敌。
     */
    @Override
    public boolean isInvulnerableTo(
            DamageSource damageSource
    ) {
        return SupernaturalCombatHandler.isInvulnerableTo(
                this,
                damageSource
        );
    }

    @Override
    public boolean isSupernaturallyStunned() {
        return permanentSupernaturalStun
                || supernaturalStunTicks > 0;
    }

    /**
     * 夜游鬼白天自动消失
     */
    @Override
    public void tick() {
        super.tick();

        // 白天自动消失
        if (!level().isClientSide() && level().isDay()) {
            discard();
        }
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
                        NORMAL_SPEED
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

            return !mob.isSupernaturallyStunned()
                    && mob.supernaturalAttackCooldown <= 0
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

            return !mob.isSupernaturallyStunned()
                    && mob.supernaturalAttackCooldown <= 0
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
                    ModDamageTypes.ghostNightWanderer(mob),
                    SUPERNATURAL_ATTACK_STRENGTH
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