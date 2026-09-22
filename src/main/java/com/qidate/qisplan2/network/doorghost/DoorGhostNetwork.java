package com.qidate.qisplan2.network.doorghost;

import com.qidate.qisplan2.client.DoorGhostMarkClient;
import com.qidate.qisplan2.ghost.ability.doorghost.DoorGhostAbilityHandler;
import com.qidate.qisplan2.network.payload.DoorGhostAbilityPayload;
import com.qidate.qisplan2.network.payload.DoorGhostMarkPayload;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public final class DoorGhostNetwork {

    private DoorGhostNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");

        /*
         * ========================================================
         * S2C：门鬼标记
         * ========================================================
         */

        registrar.playToClient(
                DoorGhostMarkPayload.TYPE,
                DoorGhostMarkPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        DoorGhostMarkClient.apply(
                                payload.entityId(),
                                payload.marked()
                        );
                    });
                }
        );

        /*
         * ========================================================
         * C2S：门鬼能力
         * ========================================================
         */

        registrar.playToServer(
                DoorGhostAbilityPayload.TYPE,
                DoorGhostAbilityPayload.STREAM_CODEC,
                DoorGhostNetwork::handleAbility
        );
    }

    private static void handleAbility(
            DoorGhostAbilityPayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            DoorGhostAbilityHandler.use(
                    player
            );
        });
    }

    /*
     * ========================================================
     * S2C：标记
     * ========================================================
     */

    public static void sendMark(
            ServerPlayer player,
            UUID target,
            boolean marked
    ) {

        Entity entity =
                player.serverLevel().getEntity(
                        target
                );

        if (entity == null) {
            return;
        }

        PacketDistributor.sendToPlayer(
                player,
                new DoorGhostMarkPayload(
                        entity.getId(),
                        marked
                )
        );
    }

    /*
     * ========================================================
     * C2S：使用门鬼能力
     * ========================================================
     */

    public static void sendAbility() {

        PacketDistributor.sendToServer(
                new DoorGhostAbilityPayload()
        );
    }
}