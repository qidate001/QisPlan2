package com.qidate.qisplan2.network;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.GhostPianoMusicClient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(
        modid = QisPlan2.MODID
)
public final class QisNetwork {

    private QisNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");

        /*
         * 服务端 → 客户端：
         * 开始播放鬼音乐
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
         * 服务端 → 客户端：
         * 停止播放鬼音乐
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
}