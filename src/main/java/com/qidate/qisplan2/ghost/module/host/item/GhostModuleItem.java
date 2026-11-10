package com.qidate.qisplan2.ghost.module.host.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 普通物品形态的模块载体，默认不安装模块。
 * 子类可通过物品组件配置初始模块；交互统一由 GhostModuleItemSupport 提供。
 */
public class GhostModuleItem extends Item implements GhostModuleItemSupport {
    public GhostModuleItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return useModules(level, player, hand);
    }
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        tickModules(stack, level, entity);
    }

}
