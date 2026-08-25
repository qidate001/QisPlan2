package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.PossessionHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class PossessionAbilityHandler {

    @SubscribeEvent
    public static void onAttack(
            AttackEntityEvent event
    ) {

        /*
         * 必须是服务器玩家。
         */
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        /*
         * 必须空手。
         */
        ItemStack mainHand =
                player.getMainHandItem();

        if (!mainHand.isEmpty()) {
            return;
        }

        /*
         * 目标必须是 LivingEntity。
         */
        if (!(event.getTarget()
                instanceof LivingEntity target)) {
            return;
        }

        /*
         * 当前必须驾驭夜游鬼。
         */
        if (!PossessionHandler.hasGhost(
                player,
                PossessionHandler.NIGHT_WANDERER
        )) {
            return;
        }

        /*
         * ========================================
         * 这次不是普通拳击
         *
         * 而是夜游鬼能力。
         * ========================================
         */

        event.setCanceled(true);

        PossessionHandler.useAbility(
                player,
                PossessionHandler.NIGHT_WANDERER,
                target
        );
    }
}