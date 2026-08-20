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

        ServerPlayer player =
                event.getEntity()
                        instanceof ServerPlayer serverPlayer
                        ? serverPlayer
                        : null;

        if (player == null) {
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
         * 当前目标必须是 LivingEntity。
         */
        if (!(event.getTarget()
                instanceof LivingEntity target)) {
            return;
        }

        /*
         * 目前只处理夜游鬼。
         */
        if (!PossessionHandler.hasGhost(
                player,
                PossessionHandler.NIGHT_WANDERER
        )) {
            return;
        }

        /*
         * 阻止原版普通空手攻击。
         */
        event.setCanceled(true);

        /*
         * 使用夜游鬼能力。
         */
        PossessionHandler.useNightWandererAbility(
                player,
                target
        );
    }
}