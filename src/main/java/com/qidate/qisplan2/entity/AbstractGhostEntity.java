package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.SupernaturalCombatHandler;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.entity.ai.GhostWanderGoal;
import com.qidate.qisplan2.ghost.possession.GhostPossessionInteractionSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
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
     * 公共灵异属性
     * ========================================
     */

    /**
     * 灵异强度。
     */
    private double supernaturalStrength = 0.0D;

    /**
     * 灵异防御。
     */
    private double supernaturalDefense = 0.0D;


    /*
     * ========================================
     * 驾驭 ID
     * ========================================
     */

    private static final ResourceLocation UNKNOWN_GHOST_ID =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "unknown_ghost"
            );

    /**
     * 这只鬼对应的驾驭 ID。
     *
     * 未实现驾驭的鬼默认返回 unknown_ghost。
     */
    public ResourceLocation getGhostId() {
        return UNKNOWN_GHOST_ID;
    }


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

    private static final String COFFIN_NAIL_KEY =
            "QisPlan2CoffinNailed";

    private static final String NBT_SUPERNATURAL_STRENGTH =
            "QisPlan2SupernaturalStrength";

    private static final String NBT_SUPERNATURAL_DEFENSE =
            "QisPlan2SupernaturalDefense";


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
        return GhostStateSystem.getRevival(this);
    }

    public void setRevival(
            double value
    ) {
        GhostStateSystem.setRevival(
                this,
                value
        );
    }

    public void addRevival(
            double value
    ) {
        GhostStateSystem.addRevival(
                this,
                value
        );
    }

    /**
     * 获取内部保存的复苏值。
     *
     * <p>
     * 仅供 {@link GhostStateSystem} 使用。
     */
    double getRevivalValue() {
        return revival;
    }

    /**
     * 设置内部保存的复苏值。
     *
     * @param value 新的复苏值
     */
    void setRevivalValue(
            double value
    ) {
        revival = Math.max(
                0.0D,
                value
        );
    }


    /*
     * ========================================
     * 普通死机
     * ========================================
     */

    public int getSupernaturalStunTicks() {
        return GhostStateSystem.getSupernaturalStunTicks(
                this
        );
    }

    public void setSupernaturalStunTicks(
            int ticks
    ) {
        GhostStateSystem.setSupernaturalStunTicks(
                this,
                ticks
        );
    }

    public void addSupernaturalStunTicks(
            int ticks
    ) {
        GhostStateSystem.addSupernaturalStunTicks(
                this,
                ticks
        );
    }

    /**
     * 获取内部保存的普通死机时间。
     */
    int getSupernaturalStunTicksValue() {
        return supernaturalStunTicks;
    }

    /**
     * 设置内部保存的普通死机时间。
     *
     * @param ticks 死机时间
     */
    void setSupernaturalStunTicksValue(
            int ticks
    ) {
        supernaturalStunTicks = ticks;
    }


    /*
     * ========================================
     * 永久死机
     * ========================================
     */

    @Override
    public boolean isPermanentlySupernaturallyStunned() {
        return GhostStateSystem
                .isPermanentlySupernaturallyStunned(this);
    }

    public void setPermanentSupernaturalStun(
            boolean value
    ) {
        GhostStateSystem.setPermanentSupernaturalStun(
                this,
                value
        );
    }

    /**
     * 获取内部保存的永久死机状态。
     */
    boolean isPermanentlySupernaturallyStunnedValue() {
        return permanentSupernaturalStun;
    }

    /**
     * 设置内部保存的永久死机状态。
     *
     * @param value 是否永久死机
     */
    void setPermanentSupernaturalStunValue(
            boolean value
    ) {
        permanentSupernaturalStun = value;
    }


    /*
     * ========================================
     * 棺材钉
     * ========================================
     */

    /**
     * 是否被棺材钉钉住。
     */
    public boolean isCoffinNailed() {

        return getPersistentData().getBoolean(
                COFFIN_NAIL_KEY
        );
    }

    /**
     * 设置棺材钉状态。
     */
    public void setCoffinNailed(
            boolean nailed
    ) {

        getPersistentData().putBoolean(
                COFFIN_NAIL_KEY,
                nailed
        );
    }


    /*
     * ========================================
     * 当前是否死机
     * ========================================
     */

    @Override
    public boolean isSupernaturallyStunned() {
        return GhostStateSystem.isSupernaturallyStunned(
                this
        );
    }

    /**
     * 获取内部保存的灵异强度。
     */
    protected double getSupernaturalStrengthValue() {
        return supernaturalStrength;
    }

    /**
     * 设置内部保存的灵异强度。
     *
     * @param value 灵异强度
     */
    protected void setSupernaturalStrengthValue(
            double value
    ) {
        supernaturalStrength = Math.max(
                0.0D,
                value
        );
    }

    /**
     * 获取内部保存的灵异防御。
     */
    protected double getSupernaturalDefenseValue() {
        return supernaturalDefense;
    }

    /**
     * 设置内部保存的灵异防御。
     *
     * @param value 灵异防御
     */
    protected void setSupernaturalDefenseValue(
            double value
    ) {
        supernaturalDefense = Math.max(
                0.0D,
                value
        );
    }


    /*
     * ========================================
     * 灵异属性
     * ========================================
     */

    /**
     * 获取当前灵异强度。
     *
     * <p>
     * 所有实体鬼统一通过 {@link GhostAttributeSystem}
     * 管理该属性。
     */
    @Override
    public double getSupernaturalStrength() {
        return GhostAttributeSystem.getSupernaturalStrength(this);
    }

    /**
     * 获取当前灵异防御。
     *
     * <p>
     * 所有实体鬼统一通过 {@link GhostAttributeSystem}
     * 管理该属性。
     */
    @Override
    public double getSupernaturalDefense() {
        return GhostAttributeSystem.getSupernaturalDefense(this);
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
        if (GhostStateSystem.isPermanentlySupernaturallyStunned(this)) {
            return;
        }

        GhostStateSystem.addSupernaturalStunTicks(
                this,
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

        GhostStateSystem.permanentlyStun(this);

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
    public final void aiStep() {

        super.aiStep();

        if (GhostStateSystem.tick(this)) {

            getNavigation().stop();
            setTarget(null);
            setAggressive(false);

            return;
        }

        /*
         * ========================================================
         * 子类 AI
         * ========================================================
         */
        tickGhostAI();
    }

    protected void tickGhostAI() {}


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

        tag.putDouble(
                NBT_SUPERNATURAL_STRENGTH,
                supernaturalStrength
        );

        tag.putDouble(
                NBT_SUPERNATURAL_DEFENSE,
                supernaturalDefense
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

        supernaturalStrength =
                Math.max(
                        0.0D,
                        tag.getDouble(
                                NBT_SUPERNATURAL_STRENGTH
                        )
                );

        supernaturalDefense =
                Math.max(
                        0.0D,
                        tag.getDouble(
                                NBT_SUPERNATURAL_DEFENSE
                        )
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
        GhostStateSystem.clearSupernaturalStun(
                this
        );
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

        return GhostPossessionInteractionSystem.handle(
                this,
                player,
                hand
        );
    }

    @Override
    protected void registerGoals() {

        /*
         * ========================================================
         * 所有厉鬼的默认游荡行为
         * ========================================================
         */
        this.goalSelector.addGoal(
                8,
                new GhostWanderGoal(
                        this,
                        0.7D
                )
        );
    }
}