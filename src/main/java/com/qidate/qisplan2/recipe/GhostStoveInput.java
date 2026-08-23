package com.qidate.qisplan2.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public record GhostStoveInput(
        List<ItemStack> items
) implements RecipeInput {

    public static final int SIZE = 5;

    public GhostStoveInput {
        if (items.size() != SIZE) {
            throw new IllegalArgumentException(
                    "GhostStoveInput 必须有 5 个槽位，实际：" +
                            items.size()
            );
        }
    }

    @Override
    public ItemStack getItem(
            int slot
    ) {
        return items.get(slot);
    }

    @Override
    public int size() {
        return SIZE;
    }
}