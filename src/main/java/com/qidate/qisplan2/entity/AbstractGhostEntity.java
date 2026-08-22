package com.qidate.qisplan2.entity;

import com.qidate.qisplan2.death.SupernaturalEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * 所有实体厉鬼的公共基类。
 *
 * 负责：
 * 1. 复苏值
 * 2. 浅死机值
 * 3. 永久死机
 * 4. 灵异防御强度
 * 5. 灵异攻击后的死机处理
 * 6. NBT 永久保存
 */
public abstract class AbstractGhostEntity
        extends Monster
        implements SupernaturalEntity {

    /**
     * 复苏值。
     */
    private double revival = 0.0D;

    /**
     * 浅死机值。
     */
    private double shallowStun = 0.0D;

    /**
     * 是否永久死机。
     */
    private boolean permanentlySupernaturallyStunned = false;

    protected AbstractGhostEntity(
            EntityType<? extends Monster> entityType,
            Level level
    ) {
        super(
                entityType,
                level
        );
    }

    // =========================================================
    // 复苏
    // =========================================================

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

    // =========================================================
    // 浅死机
    // =========================================================

    public double getShallowStun() {
        return shallowStun;
    }

    public void setShallowStun(
            double value
    ) {
        shallowStun = Math.max(
                0.0D,
                value
        );
    }

    public void addShallowStun(
            double value
    ) {
        setShallowStun(
                shallowStun + value
        );
    }

    // =========================================================
    // 永久死机
    // =========================================================

    @Override
    public boolean isPermanentlySupernaturallyStunned() {
        return permanentlySupernaturallyStunned;
    }

    public void setPermanentlySupernaturallyStunned(
            boolean value
    ) {
        permanentlySupernaturallyStunned = value;
    }

    // =========================================================
    // 当前是否处于死机
    // =========================================================

    @Override
    public boolean isSupernaturallyStunned() {
        return permanentlySupernaturallyStunned
                || shallowStun > 0.0D;
    }

    // =========================================================
    // 灵异防御
    // =========================================================

    /**
     * 默认灵异防御。
     *
     * 之后夜游鬼可以 override。
     */
    @Override
    public double getSupernaturalDefense() {
        return 0.0D;
    }

    // =========================================================
    // 普通灵异攻击
    // =========================================================

    @Override
    public void onSupernaturalAttack(
            int ticks
    ) {
        if (permanentlySupernaturallyStunned) {
            return;
        }

        addShallowStun(
                ticks
        );
    }

    // =========================================================
    // 永久灵异攻击
    // =========================================================

    @Override
    public void onPermanentSupernaturalAttack() {
        permanentlySupernaturallyStunned = true;
    }

    // =========================================================
    // Tick
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        /*
         * 永久死机不需要处理倒计时。
         */
        if (permanentlySupernaturallyStunned) {
            shallowStun = 0.0D;
            return;
        }

        /*
         * 每 tick 减少浅死机时间。
         */
        if (shallowStun > 0.0D) {
            shallowStun = Math.max(
                    0.0D,
                    shallowStun - 1.0D
            );
        }
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {
        super.addAdditionalSaveData(
                tag
        );

        tag.putDouble(
                "GhostRevival",
                revival
        );

        tag.putDouble(
                "GhostShallowStun",
                shallowStun
        );

        tag.putBoolean(
                "GhostPermanentlySupernaturallyStunned",
                permanentlySupernaturallyStunned
        );
    }

    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {
        super.readAdditionalSaveData(
                tag
        );

        revival =
                tag.getDouble(
                        "GhostRevival"
                );

        shallowStun =
                tag.getDouble(
                        "GhostShallowStun"
                );

        permanentlySupernaturallyStunned =
                tag.getBoolean(
                        "GhostPermanentlySupernaturallyStunned"
                );
    }
}