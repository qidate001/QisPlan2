package com.qidate.qisplan2.item.candle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

/**
 * 白色鬼烛。
 *
 * <p>
 * 点燃后用于产生避鬼效果。
 *
 * <p>
 * 当前负责维护鬼烛自身的点燃状态以及基础燃烧。
 * 具体避鬼效果由后续鬼烛系统负责。
 */
public class WhiteGhostCandleItem extends Item {

    public WhiteGhostCandleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {

            if (isLit(stack)) {

                stack.remove(DataComponents.CUSTOM_MODEL_DATA);

            } else {

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

        if (!(entity instanceof Player player)) {
            return;
        }

        double burnDamage =
                GhostCandleSystem.getBurnDamagePerTick(
                        player
                );

        int damage =
                GhostCandleSystem.rollBurnDamage(
                        player,
                        burnDamage
                );

        if (damage <= 0) {
            return;
        }

        int currentDamage =
                stack.getDamageValue();

        if (damage + 1 >= stack.getMaxDamage()) {

            stack.shrink(1);
            return;
        }

        stack.setDamageValue(
                currentDamage + damage
        );
    }

    public static boolean isLit(ItemStack stack) {
        CustomModelData customModelData =
                stack.get(DataComponents.CUSTOM_MODEL_DATA);

        return customModelData != null;
    }
}