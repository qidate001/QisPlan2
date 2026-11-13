package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.module.host.item.GhostModuleItemSupport;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** 非玩家装备没有玩家背包 tick，由此桥接模块物品生命周期。 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class GhostEquipmentModuleHandler {
    /** 仅提供静态事件处理。 */
    private GhostEquipmentModuleHandler() {}

    /** 玩家由物品 inventoryTick 处理，此处仅补齐其他生物，避免重复计时。 */
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity instanceof Player
                || entity.level().isClientSide() || !entity.isAlive()) return;
        for (var stack : entity.getAllSlots())
            if (!stack.isEmpty() && stack.getItem() instanceof GhostModuleItemSupport item)
                item.tickModules(stack, entity.level(), entity);
    }
}
