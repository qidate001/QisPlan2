package com.qidate.qisplan2.item;

import com.qidate.qisplan2.client.GhostBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GhostBookItem extends Item {

    public GhostBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        /*
         * 只在客户端打开 GUI。
         */
        if (level.isClientSide()) {

            Minecraft.getInstance().setScreen(
                    new GhostBookScreen()
            );
        }

        return InteractionResultHolder.sidedSuccess(
                player.getItemInHand(hand),
                level.isClientSide()
        );
    }
}