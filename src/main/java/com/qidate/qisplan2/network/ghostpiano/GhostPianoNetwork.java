package com.qidate.qisplan2.network.ghostpiano;

import com.qidate.qisplan2.client.GhostPianoMusicClient;
import com.qidate.qisplan2.network.payload.StartGhostPianoMusicPayload;
import com.qidate.qisplan2.network.payload.StopGhostPianoMusicPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class GhostPianoNetwork {

    private GhostPianoNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");

        /*
         * ========================================================
         * 鬼钢琴：开始播放
         * ========================================================
         */

        registrar.playToClient(
                StartGhostPianoMusicPayload.TYPE,
                StartGhostPianoMusicPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        GhostPianoMusicClient.start(
                                payload.pos()
                        );

                    });
                }
        );

        /*
         * ========================================================
         * 鬼钢琴：停止播放
         * ========================================================
         */

        registrar.playToClient(
                StopGhostPianoMusicPayload.TYPE,
                StopGhostPianoMusicPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        GhostPianoMusicClient.stop(
                                payload.pos()
                        );

                    });
                }
        );
    }

    /*
     * ========================================================
     * S2C：开始播放
     * ========================================================
     */

    public static void sendStart(
            ServerPlayer player,
            BlockPos pos
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new StartGhostPianoMusicPayload(
                        pos
                )
        );
    }

    /*
     * ========================================================
     * S2C：停止播放
     * ========================================================
     */

    public static void sendStop(
            ServerPlayer player,
            BlockPos pos
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new StopGhostPianoMusicPayload(
                        pos
                )
        );
    }
}