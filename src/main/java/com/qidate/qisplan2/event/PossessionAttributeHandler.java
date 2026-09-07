package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.PossessionHandler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(
        modid = QisPlan2.MODID
)
public final class PossessionAttributeHandler {

    private static final ResourceLocation
            POSSESSION_MAX_HEALTH =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "possession_max_health"
            );

    private PossessionAttributeHandler() {
    }

    @SubscribeEvent
    public static void playerTick(
            PlayerTickEvent.Post event
    ) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AttributeInstance maxHealth =
                player.getAttribute(
                        Attributes.MAX_HEALTH
                );

        if (maxHealth == null) {
            return;
        }

        double bonus =
                PossessionHandler.getMaxHealthBonus(
                        player
                );

        maxHealth.addOrReplacePermanentModifier(
                new AttributeModifier(
                        POSSESSION_MAX_HEALTH,
                        bonus,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );

        /*
         * 防止当前生命超过新的上限。
         */
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(
                    player.getMaxHealth()
            );
        }
    }
}