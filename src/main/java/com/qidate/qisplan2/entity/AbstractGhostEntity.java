package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.death.SupernaturalCombatHandler;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.ghost.GhostPossessionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 所有实体鬼的公共基类。
 *
 * 负责：
 * - 复苏值
 * - 普通死机
 * - 永久死机
 * - 灵异防御
 * - 对应 NBT 持久化
 */
public abstract class AbstractGhostEntity
        extends PathfinderMob
        implements SupernaturalEntity {

    /*
     * ========================================
     * 公共灵异状态
     * ========================================
     */

    /**
     * 复苏值。
     *
     * 这里只负责保存与修改，
     * 不决定具体复苏规则。
     */
    private double revival = 0.0D;

    /**
     * 当前普通死机剩余时间。
     */
    protected int supernaturalStunTicks = 0;

    /**
     * 是否永久死机。
     */
    protected boolean permanentSupernaturalStun = false;


    /*
     * ========================================
     * NBT
     * ========================================
     */

    private static final String NBT_REVIVAL =
            "QisPlan2Revival";

    private static final String NBT_STUN_TICKS =
            "QisPlan2SupernaturalStunTicks";

    private static final String NBT_PERMANENT_STUN =
            "QisPlan2PermanentSupernaturalStun";


    protected AbstractGhostEntity(
            EntityType<? extends PathfinderMob> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );
    }


    /*
     * ========================================
     * 复苏值
     * ========================================
     */

    public double getRevival() {
        return revival;
    }

    public void setRevival(
            double value
    ) {
        revival = Math.max(
                0.0D,
                value
        );
    }

    public void addRevival(
            double value
    ) {
        setRevival(
                revival + value
        );
    }


    /*
     * ========================================
     * 普通死机
     * ========================================
     */

    public int getSupernaturalStunTicks() {
        return supernaturalStunTicks;
    }

    public void setSupernaturalStunTicks(
            int ticks
    ) {
        supernaturalStunTicks =
                Math.max(
                        0,
                        ticks
                );
    }

    public void addSupernaturalStunTicks(
            int ticks
    ) {
        long result =
                (long) supernaturalStunTicks
                        + ticks;

        supernaturalStunTicks =
                (int) Math.min(
                        Integer.MAX_VALUE,
                        Math.max(
                                0L,
                                result
                        )
                );
    }


    /*
     * ========================================
     * 永久死机
     * ========================================
     */

    @Override
    public boolean isPermanentlySupernaturallyStunned() {
        return permanentSupernaturalStun;
    }

    public void setPermanentSupernaturalStun(
            boolean value
    ) {
        permanentSupernaturalStun = value;

        if (value) {
            supernaturalStunTicks = 0;
        }
    }


    /*
     * ========================================
     * 当前是否死机
     * ========================================
     */

    @Override
    public boolean isSupernaturallyStunned() {
        return permanentSupernaturalStun
                || supernaturalStunTicks > 0;
    }


    /*
     * ========================================
     * 灵异防御
     * ========================================
     *
     * 子类可以 override。
     */
    @Override
    public double getSupernaturalDefense() {
        return 0.0D;
    }


    /*
     * ========================================
     * 普通灵异攻击
     * ========================================
     */

    @Override
    public void onSupernaturalAttack(
            int ticks
    ) {
        if (permanentSupernaturalStun) {
            return;
        }

        addSupernaturalStunTicks(
                ticks
        );

        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
    }


    /*
     * ========================================
     * 永久灵异攻击
     * ========================================
     */

    @Override
    public void onPermanentSupernaturalAttack() {

        permanentSupernaturalStun = true;

        supernaturalStunTicks = 0;

        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
    }


    /*
     * ========================================
     * 公共 Tick
     * ========================================
     *
     * 这里处理所有实体鬼共有的死机倒计时。
     */
    @Override
    public void aiStep() {

        super.aiStep();

        if (permanentSupernaturalStun) {

            getNavigation().stop();
            setTarget(null);
            setAggressive(false);

            return;
        }

        if (supernaturalStunTicks > 0) {

            supernaturalStunTicks--;

            getNavigation().stop();
            setTarget(null);
            setAggressive(false);

            return;
        }
    }


    /*
     * ========================================
     * NBT 保存
     * ========================================
     */

    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(tag);

        tag.putDouble(
                NBT_REVIVAL,
                revival
        );

        tag.putInt(
                NBT_STUN_TICKS,
                supernaturalStunTicks
        );

        tag.putBoolean(
                NBT_PERMANENT_STUN,
                permanentSupernaturalStun
        );
    }

    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(tag);

        revival =
                Math.max(
                        0.0D,
                        tag.getDouble(
                                NBT_REVIVAL
                        )
                );

        supernaturalStunTicks =
                Math.max(
                        0,
                        tag.getInt(
                                NBT_STUN_TICKS
                        )
                );

        permanentSupernaturalStun =
                tag.getBoolean(
                        NBT_PERMANENT_STUN
                );

        /*
         * 永久死机优先。
         */
        if (permanentSupernaturalStun) {
            supernaturalStunTicks = 0;
        }
    }

    /*
     * 离玩家很远也不自然消失
     */
    @Override
    public boolean removeWhenFarAway(
            double distanceToClosestPlayer
    ) {
        return false;
    }

    /*
     * 需要持久保存的自定义实体
     */
    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    /*
     * 和平难度也不因为和平模式自动消失
     */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /*
     * 清除普通死机状态。
     *
     * 永久死机不应该被这个方法清除。
     */
    @Override
    public void clearSupernaturalStun() {

        supernaturalStunTicks = 0;
    }

    /*
     * 无敌特性
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
    public InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {

        /*
         * ========================================================
         * 只允许主手
         * ========================================================
         */
        if (hand != InteractionHand.MAIN_HAND) {

            return super.mobInteract(
                    player,
                    hand
            );
        }

        /*
         * ========================================================
         * 必须空手
         * ========================================================
         */
        if (!player.getItemInHand(hand).isEmpty()) {

            return super.mobInteract(
                    player,
                    hand
            );
        }

        /*
         * ========================================================
         * 服务端真正开始驾驭
         * ========================================================
         */
        if (!level().isClientSide()
                && player instanceof ServerPlayer serverPlayer) {

            boolean started =
                    GhostPossessionManager.start(
                            serverPlayer,
                            this
                    );

            if (started) {

                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.sidedSuccess(
                level().isClientSide()
        );
    }
}