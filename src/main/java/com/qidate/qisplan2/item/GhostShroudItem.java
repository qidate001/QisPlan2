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

    /**
     * 无限耐久。
     */
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    /**
     * 即使带有绑定诅咒，也不显示附魔光效。
     */
    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}