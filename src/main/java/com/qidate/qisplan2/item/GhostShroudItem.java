package com.qidate.qisplan2.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GhostShroudItem extends ArmorItem {

    public GhostShroudItem(
            Holder<ArmorMaterial> material,
            Type type,
            Item.Properties properties
    ) {
        super(
                material,
                type,
                properties
        );
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }
}