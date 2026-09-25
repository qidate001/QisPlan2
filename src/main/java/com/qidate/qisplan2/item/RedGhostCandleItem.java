package com.qidate.qisplan2.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

/**
 * 红色鬼烛。
 *
 * <p>
 * 点燃后用于吸引附近厉鬼。
 *
 * <p>
 * 当前负责维护鬼烛自身的点燃状态以及基础燃烧。
 * 具体吸引厉鬼的效果由后续鬼烛系统负责。
 */
public class RedGhostCandleItem extends Item {

    private static final int BURN_INTERVAL = 20;

    public RedGhostCandleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!isLit(stack)) {
            if (!level.isClientSide()) {
                stack.set(
                        DataComponents.CUSTOM_MODEL_DATA,
                        new CustomModelData(1)
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide()
        );
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            net.minecraft.world.entity.Entity entity,
            int slot,
            boolean selected
    ) {
        if (level.isClientSide()) {
            return;
        }

        if (!isLit(stack)) {
            return;
        }

        if (entity.tickCount % BURN_INTERVAL != 0) {
            return;
        }

        int damage = stack.getDamageValue();

        if (damage + 1 >= stack.getMaxDamage()) {
            stack.setDamageValue(stack.getMaxDamage());
            extinguish(stack);
            return;
        }

        stack.setDamageValue(damage + 1);
    }

    public static boolean isLit(ItemStack stack) {
        CustomModelData customModelData =
                stack.get(DataComponents.CUSTOM_MODEL_DATA);

        return customModelData != null;
    }

    private static void extinguish(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }
}