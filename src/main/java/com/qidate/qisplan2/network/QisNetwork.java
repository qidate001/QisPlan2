package com.qidate.qisplan2.network;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.entity.GhostDoorPlateBlockEntity;
import com.qidate.qisplan2.client.DoorGhostMarkClient;
import com.qidate.qisplan2.client.GhostPianoMusicClient;
import com.qidate.qisplan2.client.GhostPossessionClientState;
import com.qidate.qisplan2.client.screen.GhostPossessionScreen;
import com.qidate.qisplan2.client.screen.GhostDoorPlateScreen;
import com.qidate.qisplan2.ghost.GhostPossessionSession;
import com.qidate.qisplan2.ghost.ability.doorghost.DoorGhostAbilityHandler;
import com.qidate.qisplan2.network.payload.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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

        registrar.playToServer(
                DoorGhostAbilityPayload.TYPE,
                DoorGhostAbilityPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        if (context.player()
                                instanceof net.minecraft.server.level.ServerPlayer player) {

                            DoorGhostAbilityHandler.use(
                                    player
                            );
                        }
                    });
                }
        );

        /*
         * ========================================================
         * 鬼门牌
         * ========================================================
         */

        registrar.playToClient(
                OpenGhostDoorPlateScreenPayload.TYPE,
                OpenGhostDoorPlateScreenPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        Minecraft.getInstance().setScreen(
                                new GhostDoorPlateScreen(
                                        payload.pos()
                                )
                        );
                    });
                }
        );

        registrar.playToServer(
                SetGhostDoorPlateNumberPayload.TYPE,
                SetGhostDoorPlateNumberPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        if (!(context.player()
                                instanceof ServerPlayer player)) {

                            return;
                        }

                        if (!(player.level()
                                .getBlockEntity(payload.pos())
                                instanceof GhostDoorPlateBlockEntity blockEntity)) {

                            return;
                        }

                        /*
                         * 防止客户端随便修改世界里的其他位置。
                         */
                        if (player.distanceToSqr(
                                payload.pos().getX() + 0.5D,
                                payload.pos().getY() + 0.5D,
                                payload.pos().getZ() + 0.5D
                        ) > 64.0D) {

                            return;
                        }

                        blockEntity.setNumber(
                                payload.number()
                        );
                    });
                }
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

            Minecraft.getInstance().setScreen(
                    new GhostPossessionScreen()
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

            Minecraft minecraft =
                    Minecraft.getInstance();

            if (minecraft.screen
                    instanceof GhostPossessionScreen) {

                minecraft.setScreen(null);
            }

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

    public static void sendOpenGhostDoorPlateScreen(
            ServerPlayer player,
            BlockPos pos
    ) {

        PacketDistributor.sendToPlayer(
                player,
                new OpenGhostDoorPlateScreenPayload(
                        pos
                )
        );
    }

    public static void sendSetGhostDoorPlateNumber(
            BlockPos pos,
            int number
    ) {

        PacketDistributor.sendToServer(
                new SetGhostDoorPlateNumberPayload(
                        pos,
                        number
                )
        );
    }
}