package com.qidate.qisplan2.network.ghosttombstone;

import com.qidate.qisplan2.block.entity.GhostTombstoneBlockEntity;
import com.qidate.qisplan2.client.screen.GhostTombstoneScreen;
import com.qidate.qisplan2.ghost.tombstone.GhostTombstoneInscriptionSystem;
import com.qidate.qisplan2.network.payload.OpenGhostTombstoneScreenPayload;

import com.qidate.qisplan2.network.payload.StartGhostTombstoneInscriptionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class GhostTombstoneNetwork {

    private GhostTombstoneNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");

        /*
         * ========================================================
         * S2C：打开鬼墓碑刻字界面
         * ========================================================
         */

        registrar.playToClient(
                OpenGhostTombstoneScreenPayload.TYPE,
                OpenGhostTombstoneScreenPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        Minecraft.getInstance().setScreen(
                                new GhostTombstoneScreen(
                                        payload.pos()
                                )
                        );
                    });
                }
        );

        /*
         * ========================================================
         * C2S：设置鬼墓碑刻字
         * ========================================================
         */

        registrar.playToServer(
                StartGhostTombstoneInscriptionPayload.TYPE,
                StartGhostTombstoneInscriptionPayload.STREAM_CODEC,
                GhostTombstoneNetwork::handleStartInscription
        );
    }

    /**
     * 服务端处理刻字。
     */
    private static void handleStartInscription(
            StartGhostTombstoneInscriptionPayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            if (!(player.level()
                    .getBlockEntity(
                            payload.pos()
                    )
                    instanceof GhostTombstoneBlockEntity blockEntity)) {

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

            /*
             * 空白文字没有实际刻字内容。
             */
            if (
                    payload.inscription()
                            .isBlank()
            ) {

                return;
            }

            /*
             * ========================================================
             * 开始服务器刻字过程
             * ========================================================
             */
            GhostTombstoneInscriptionSystem.start(
                    player.server,
                    player,
                    blockEntity,
                    payload.inscription()
            );
        });
    }

    /*
     * ========================================================
     * S2C：打开鬼墓碑界面
     * ========================================================
     */

    public static void sendOpenScreen(
            ServerPlayer player,
            BlockPos pos
    ) {

        PacketDistributor.sendToPlayer(
                player,
                new OpenGhostTombstoneScreenPayload(
                        pos
                )
        );
    }

    /*
     * ========================================================
     * C2S：保存刻字
     * ========================================================
     */

    public static void sendStartInscription(
            BlockPos pos,
            String inscription
    ) {

        PacketDistributor.sendToServer(
                new StartGhostTombstoneInscriptionPayload(
                        pos,
                        inscription
                )
        );
    }
}