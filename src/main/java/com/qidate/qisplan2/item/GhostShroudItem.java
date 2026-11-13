package com.qidate.qisplan2.item;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.host.item.GhostModuleItemSupport;
import com.qidate.qisplan2.ghost.module.implementation.GhostShroudModule;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GhostShroudItem extends ArmorItem implements GhostModuleItemSupport {
    /** 声明默认模块；模块被驾驭移除后不会由物品 tick 重新补回。 */
    private static List<GhostModuleData.Entry> defaultModules() {
        var initial = new GhostShroudModule().getInitialData();
        return List.of(new GhostModuleData.Entry(GhostShroudModule.ID, initial.intensity(), initial.revival()));
    }

    /** 普通使用保留原版穿戴，潜行使用接入通用模块驾驭入口。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return player.isShiftKeyDown() ? useModules(level, player, hand) : super.use(level, player, hand);
    }

    /** 玩家物品生命周期转译为模块 tick，扣血逻辑由模块处理。 */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        tickModules(stack, level, entity);
    }


    /** 保留盔甲形态，并配置自带的完整鬼寿衣模块。 */
    public GhostShroudItem(
            Holder<ArmorMaterial> material,
            Type type,
            Item.Properties properties
    ) {
        super(
                material,
                type,
                properties.component(ModDataComponents.GHOST_MODULES.get(), defaultModules())
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