package com.qidate.qisplan2.network;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.GhostPianoMusicClient;
import com.qidate.qisplan2.client.GhostPossessionClientState;
import com.qidate.qisplan2.ghost.GhostPossessionSession;
import com.qidate.qisplan2.network.payload.GhostPossessionEndPayload;
import com.qidate.qisplan2.network.payload.GhostPossessionInputPayload;
import com.qidate.qisplan2.network.payload.GhostPossessionStartPayload;
import com.qidate.qisplan2.network.payload.GhostPossessionUpdatePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

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
         * 鬼钢琴
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

        /*
         * ========================================================
         * 驾驭小游戏
         * ========================================================
         */

        registrar.playToServer(
                GhostPossessionInputPayload.TYPE,
                GhostPossessionInputPayload.STREAM_CODEC,
                QisNetwork::handleGhostPossessionInput
        );

        registrar.playToClient(
                GhostPossessionStartPayload.TYPE,
                GhostPossessionStartPayload.STREAM_CODEC,
                QisNetwork::handleGhostPossessionStart
        );

        registrar.playToClient(
                GhostPossessionUpdatePayload.TYPE,
                GhostPossessionUpdatePayload.STREAM_CODEC,
                QisNetwork::handleGhostPossessionUpdate
        );

        registrar.playToClient(
                GhostPossessionEndPayload.TYPE,
                GhostPossessionEndPayload.STREAM_CODEC,
                QisNetwork::handleGhostPossessionEnd
        );
    }


    /*
     * ========================================================
     * C2S：玩家输入
     * ========================================================
     */
    private static void handleGhostPossessionInput(
            GhostPossessionInputPayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {
                return;
            }

            var session =
                    com.qidate.qisplan2.ghost
                            .GhostPossessionManager
                            .get(player);

            if (session == null) {
                return;
            }

            session.setLeftPressed(
                    payload.left()
            );

            session.setRightPressed(
                    payload.right()
            );

            if (payload.attempt()) {

                session.attempt();
            }
        });
    }


    /*
     * ========================================================
     * S2C：开始
     * ========================================================
     */
    private static void handleGhostPossessionStart(
            GhostPossessionStartPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {

            GhostPossessionClientState.start(
                    payload.ghostEntityId(),
                    payload.totalTicks()
            );

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 开始驾驭小游戏：鬼实体ID={}，总时间={} tick",
                    payload.ghostEntityId(),
                    payload.totalTicks()
            );
        });
    }


    /*
     * ========================================================
     * S2C：更新
     * ========================================================
     */
    private static void handleGhostPossessionUpdate(
            GhostPossessionUpdatePayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {

            GhostPossessionClientState.update(
                    payload.remainingTicks(),
                    payload.cursorPosition(),
                    payload.targetPosition(),
                    payload.success()
            );
        });
    }


    /*
     * ========================================================
     * S2C：结束
     * ========================================================
     */
    private static void handleGhostPossessionEnd(
            GhostPossessionEndPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {

            GhostPossessionClientState.end();

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 驾驭结束：{}，最终成功率={}%",
                    payload.success()
                            ? "成功"
                            : "失败",
                    payload.finalSuccess()
            );
        });
    }

    public static void sendPossessionStart(
            ServerPlayer player,
            GhostPossessionSession session
    ) {
        PacketDistributor.sendToPlayer(
                player,
                GhostPossessionStartPayload.from(
                        session
                )
        );
    }

    public static void sendPossessionUpdate(
            ServerPlayer player,
            GhostPossessionSession session
    ) {
        PacketDistributor.sendToPlayer(
                player,
                GhostPossessionUpdatePayload.from(
                        session
                )
        );
    }

    public static void sendPossessionEnd(
            ServerPlayer player,
            boolean success,
            double finalSuccess
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new GhostPossessionEndPayload(
                        success,
                        finalSuccess
                )
        );
    }

    public static void sendPossessionInput(
            boolean left,
            boolean right,
            boolean attempt
    ) {
        PacketDistributor.sendToServer(
                new GhostPossessionInputPayload(
                        left,
                        right,
                        attempt
                )
        );
    }
}