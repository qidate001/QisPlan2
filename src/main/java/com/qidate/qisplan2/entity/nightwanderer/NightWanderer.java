package com.qidate.qisplan2.entity.nightwanderer;

import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.ghost.possession.ability.nightwanderer.NightWandererAbility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class NightWanderer
        extends AbstractGhostEntity {

    @Override
    public ResourceLocation getGhostId() {
        return NightWandererAbility.ID;
    }

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
     * 初始灵异强度。
     */
    private static final double BASE_SUPERNATURAL_STRENGTH = 5.0D;

    /**
     * 每击杀一个实体增加的灵异强度。
     */
    private static final double SUPERNATURAL_STRENGTH_PER_KILL = 0.1D;

    /**
     * 击杀数量。
     */
    private int killCount = 0;

    private static final double SUPERNATURAL_DEFENSE = 6.0D;

    private static final String NBT_KILL_COUNT =
            "QisPlan2KillCount";

    @Override
    public double getSupernaturalDefense() {
        return SUPERNATURAL_DEFENSE;
    }

    public double getSupernaturalStrength() {
        return BASE_SUPERNATURAL_STRENGTH
                + killCount * SUPERNATURAL_STRENGTH_PER_KILL;
    }

    public int getKillCount() {
        return killCount;
    }

    public void onKillEntity() {
        killCount++;
    }

    public void addKillCount(int amount) {
        if (amount <= 0) {
            return;
        }

        killCount += amount;
    }

    private int getSupernaturalAttackCooldown() {
        return Math.max(
                1,
                SUPERNATURAL_ATTACK_COOLDOWN
                        - killCount / 5
        );
    }

    /**
     * 当前连续多少 tick 没有有效目标。
     *
     * <p>
     * 该状态由 {@link NightWandererHuntSystem}
     * 使用，用于判断是否应该强制发动猎杀。
     */
    private int huntNoTargetTicks = 0;

    /**
     * 当前猎杀瞬移冷却。
     *
     * <p>
     * 该状态由 {@link NightWandererHuntSystem}
     * 使用，用于限制猎杀瞬移的频率。
     */
    private int huntTeleportCooldown = 0;

    /**
     * 获取当前连续无目标时间。
     */
    public int getHuntNoTargetTicks() {
        return huntNoTargetTicks;
    }

    /**
     * 增加一 tick 的无目标时间。
     */
    public void addHuntNoTargetTick() {
        huntNoTargetTicks++;
    }

    /**
     * 清除当前无目标计时。
     *
     * <p>
     * 一旦夜游鬼重新拥有有效目标，
     * 猎杀系统就会调用这个方法。
     */
    public void resetHuntNoTargetTicks() {
        huntNoTargetTicks = 0;
    }

    /**
     * 获取当前猎杀瞬移冷却。
     */
    public int getHuntTeleportCooldown() {
        return huntTeleportCooldown;
    }

    /**
     * 设置猎杀瞬移冷却。
     *
     * @param ticks 冷却 tick 数
     */
    public void setHuntTeleportCooldown(
            int ticks
    ) {
        huntTeleportCooldown =
                Math.max(0, ticks);
    }

    /**
     * 每 tick 更新猎杀瞬移冷却。
     *
     * <p>
     * 具体什么时候调用由夜游鬼自身的 Tick 流程决定。
     */
    public void tickHuntTeleportCooldown() {
        if (huntTeleportCooldown > 0) {
            huntTeleportCooldown--;
        }
    }

    /**
     * 当前灵异攻击是否已经可以再次发动。
     */
    public boolean isSupernaturalAttackReady() {
        return supernaturalAttackCooldown <= 0;
    }

    /**
     * 发动一次灵异攻击后，开始新的冷却。
     */
    public void startSupernaturalAttackCooldown() {
        supernaturalAttackCooldown =
                getSupernaturalAttackCooldown();
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
         * 灵异攻击 Goal
         * ========================================
         */

        this.goalSelector.addGoal(
                1,
                new NightWandererSupernaturalAttackGoal(
                        this,
                        1.2D,
                        3.0D
                )
        );

        /*
         * ========================================
         * 攻击其他 LivingEntity
         * ========================================
         *
         * 玩家由 tickGhostAI() 单独优先寻找。
         *
         * 这里负责其他 LivingEntity。
         */
        this.targetSelector.addGoal(
                2,
                new NearestAttackableTargetGoal<>(
                        this,
                        LivingEntity.class,
                        32,
                        true,
                        false,
                        target -> {
                            if (target instanceof Player player) {
                                return !player.isSpectator()
                                        && !player.isCreative();
                            }

                            return true;
                        }
                )
        );
    }

    /**
     * 夜游鬼每 tick 更新自身状态。
     */
    @Override
    protected void tickGhostAI() {

        /*
         * ========================================
         * 白天
         * ========================================
         *
         * 白天不主动进行移动 AI。
         *
         * 但不会清除当前目标。
         * 因此近距离目标仍然可以被攻击。
         */
        if (level().isDay()) {
            return;
        }

        /*
         * ========================================
         * 灵异攻击冷却
         * ========================================
         */
        if (supernaturalAttackCooldown > 0) {
            supernaturalAttackCooldown--;
        }

        /*
         * ========================================
         * 猎杀瞬移冷却
         * ========================================
         */
        tickHuntTeleportCooldown();

        /*
         * ========================================
         * 玩家优先
         * ========================================
         *
         * 只有当前没有有效目标时，
         * 才主动寻找玩家。
         *
         * 这样不会把正在追杀其他实体的目标强行
         * 切换回玩家。
         */
        if (!level().isClientSide()) {

            LivingEntity currentTarget =
                    getTarget();

            if (currentTarget == null
                    || !currentTarget.isAlive()
                    || currentTarget.isRemoved()) {

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
        }

        /*
         * ========================================
         * 猎杀能力
         * ========================================
         */
        NightWandererHuntSystem.tick(this);

        /*
         * ========================================
         * 光照移速
         * ========================================
         */
        NightWandererMovementSystem.tick(this);
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

    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {

        super.addAdditionalSaveData(tag);

        tag.putInt(
                NBT_KILL_COUNT,
                killCount
        );
    }

    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {

        super.readAdditionalSaveData(tag);

        killCount =
                Math.max(
                        0,
                        tag.getInt(NBT_KILL_COUNT)
                );
    }
}