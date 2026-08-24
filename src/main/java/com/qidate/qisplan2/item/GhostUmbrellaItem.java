package com.qidate.qisplan2.item;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GhostUmbrellaItem extends Item {

    public GhostUmbrellaItem(
            Properties properties
    ) {
        super(properties);
    }

    public static boolean isOpen(
            ItemStack stack
    ) {
        Boolean value =
                stack.get(
                        QisPlan2.GHOST_UMBRELLA_OPEN
                );

        return Boolean.TRUE.equals(value);
    }

    public static void setOpen(
            ItemStack stack,
            boolean open
    ) {
        stack.set(
                QisPlan2.GHOST_UMBRELLA_OPEN,
                open
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack =
                player.getItemInHand(hand);

        setOpen(
                stack,
                !isOpen(stack)
        );

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide()
        );
    }
}