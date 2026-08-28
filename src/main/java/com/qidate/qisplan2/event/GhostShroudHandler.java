package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.item.GhostShroudItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class GhostShroudHandler {

    /**
     * 鬼寿衣每 25 秒扣 1 点生命。
     */
    private static final int DRAIN_INTERVAL = 500;

    @SubscribeEvent
    public static void onEntityTick(
            EntityTickEvent.Post event
    ) {

        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        ItemStack chest =
                player.getItemBySlot(
                        EquipmentSlot.CHEST
                );

        if (!(chest.getItem()
                instanceof GhostShroudItem)) {
            return;
        }

        ensureBindingCurse(
                player,
                chest
        );

        /*
         * 每 100 tick = 5 秒。
         */
        if (player.tickCount % DRAIN_INTERVAL != 0) {
            return;
        }

        float oldHealth =
                player.getHealth();

        player.setHealth(
                Math.max(
                        0.0F,
                        oldHealth - 1.0F
                )
        );

//        QisPlan2.LOGGER.info(
//                "[QisPlan2] 鬼寿衣扣血：{} -> {}",
//                oldHealth,
//                player.getHealth()
//        );
    }

    @SubscribeEvent
    public static void onEquipmentChange(
            LivingEquipmentChangeEvent event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.isCreative()) {
            return;
        }

        if (event.getSlot() != EquipmentSlot.CHEST) {
            return;
        }

        /*
         * 原来穿着鬼寿衣，现在胸甲槽变成其他东西。
         */
        if (!(event.getFrom().getItem() instanceof GhostShroudItem)) {
            return;
        }

        if (event.getTo().getItem() instanceof GhostShroudItem) {
            return;
        }

        ItemStack shroud = event.getFrom().copy();
        ItemStack replacement = event.getTo().copy();

        /*
         * 把鬼寿衣重新放回胸甲槽。
         */
        player.setItemSlot(
                EquipmentSlot.CHEST,
                shroud
        );

        /*
         * 玩家本来试图放进胸甲槽的新物品
         * 放回背包。
         */
        if (!replacement.isEmpty()) {
            boolean added =
                    player.addItem(replacement);

            /*
             * 背包满了就掉在玩家脚边，
             * 避免物品凭空消失。
             */
            if (!added) {
                player.drop(
                        replacement,
                        false
                );
            }
        }

//        QisPlan2.LOGGER.info(
//                "[QisPlan2] 鬼寿衣阻止脱下：{}",
//                player.getName().getString()
//        );
    }

    private static void ensureBindingCurse(
            ServerPlayer player,
            ItemStack stack
    ) {
        if (!(stack.getItem() instanceof GhostShroudItem)) {
            return;
        }

        /*
         * 已经有绑定诅咒就不重复处理。
         */
        ItemEnchantments current =
                stack.getOrDefault(
                        DataComponents.ENCHANTMENTS,
                        ItemEnchantments.EMPTY
                );

        var registry =
                player.level()
                        .registryAccess()
                        .lookupOrThrow(
                                Registries.ENCHANTMENT
                        );

        var binding =
                registry.getOrThrow(
                        net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE
                );

        /*
         * 已经存在绑定诅咒。
         */
        if (current.getLevel(binding) > 0) {
            return;
        }

        /*
         * 构造新的附魔数据。
         */
        ItemEnchantments.Mutable mutable =
                new ItemEnchantments.Mutable(
                        current
                );

        mutable.set(
                binding,
                1
        );

        stack.set(
                DataComponents.ENCHANTMENTS,
                mutable.toImmutable()
        );
    }
}