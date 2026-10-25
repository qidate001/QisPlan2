package com.qidate.qisplan2.network.ghostsound;

import com.qidate.qisplan2.client.GhostSoundClient;
import com.qidate.qisplan2.network.payload.PlayGhostSoundPayload;
import com.qidate.qisplan2.network.payload.StopGhostSoundPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class GhostSoundNetwork {

    private GhostSoundNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToClient(
                PlayGhostSoundPayload.TYPE,
                PlayGhostSoundPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        GhostSoundClient.start(
                                payload.sourceId(),
                                payload.dimension(),
                                payload.soundEvent(),
                                payload.x(),
                                payload.y(),
                                payload.z()
                        )
                )
        );

        registrar.playToClient(
                StopGhostSoundPayload.TYPE,
                StopGhostSoundPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        GhostSoundClient.stop(
                                payload.sourceId(),
                                payload.dimension()
                        )
                )
        );
    }

    public static void sendPlay(
            ServerPlayer player,
            PlayGhostSoundPayload payload
    ) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendStop(
            ServerPlayer player,
            StopGhostSoundPayload payload
    ) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}