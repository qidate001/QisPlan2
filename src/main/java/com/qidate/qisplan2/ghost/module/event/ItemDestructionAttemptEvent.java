
package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 灵异语义事件：物品摧毁尝试。
 *
 * 当物品即将因耐久耗尽而损坏时触发。
 * 事件默认允许摧毁，鬼模块可以撤销本次摧毁尝试。
 */
public final class ItemDestructionAttemptEvent
        extends DestructionAttemptEvent {

    private final ItemStack stack;

    @Nullable
    private final LivingEntity holder;

    private final int currentDamage;
    private final int damageAmount;
    private final int maxDamage;

    public ItemDestructionAttemptEvent(
            ItemStack stack,
            @Nullable LivingEntity holder,
            int currentDamage,
            int damageAmount,
            int maxDamage
    ) {
        this.stack = Objects.requireNonNull(stack, "stack");

        if (stack.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot create a destruction attempt for an empty ItemStack"
            );
        }

        if (currentDamage < 0) {
            throw new IllegalArgumentException(
                    "Current damage must be non-negative"
            );
        }

        if (damageAmount <= 0) {
            throw new IllegalArgumentException(
                    "Damage amount must be positive"
            );
        }

        if (maxDamage <= 0) {
            throw new IllegalArgumentException(
                    "Max damage must be positive"
            );
        }

        this.holder = holder;
        this.currentDamage = currentDamage;
        this.damageAmount = damageAmount;
        this.maxDamage = maxDamage;
    }

    /**
     * 即将被摧毁的物品栈。
     */
    public ItemStack getStack() {
        return stack;
    }

    /**
     * 持有该物品的实体；某些损坏路径可能没有持有者。
     */
    @Nullable
    public LivingEntity getHolder() {
        return holder;
    }

    /**
     * 本次耐久损耗发生前的耐久损伤值。
     */
    public int getCurrentDamage() {
        return currentDamage;
    }

    /**
     * 本次准备施加的实际耐久损耗。
     */
    public int getDamageAmount() {
        return damageAmount;
    }

    /**
     * 物品的最大耐久值。
     */
    public int getMaxDamage() {
        return maxDamage;
    }
}
