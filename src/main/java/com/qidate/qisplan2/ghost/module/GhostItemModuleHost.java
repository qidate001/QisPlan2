package com.qidate.qisplan2.ghost.module;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.List;

/**
 * ItemStack 的模块宿主适配器。
 *
 * 仅在当前操作期间使用，不长期保存 ItemStack 引用。
 */
public final class GhostItemModuleHost implements GhostModuleHost {

    private final ItemStack stack;

    public GhostItemModuleHost(ItemStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack");
    }

    @Override
    public List<GhostModuleData.Entry> getModules() {
        return GhostItemData.getModules(stack);
    }
}