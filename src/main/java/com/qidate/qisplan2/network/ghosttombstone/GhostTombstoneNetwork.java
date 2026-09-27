package com.qidate.qisplan2.network.ghosttombstone;

import com.qidate.qisplan2.block.entity.GhostTombstoneBlockEntity;
import com.qidate.qisplan2.client.screen.GhostTombstoneScreen;
import com.qidate.qisplan2.network.payload.OpenGhostTombstoneScreenPayload;
import com.qidate.qisplan2.network.payload.SetGhostTombstoneInscriptionPayload;

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
                SetGhostTombstoneInscriptionPayload.TYPE,
                SetGhostTombstoneInscriptionPayload.STREAM_CODEC,
                GhostTombstoneNetwork::handleSetInscription
        );
    }

    /**
     * 服务端处理刻字。
     */
    private static void handleSetInscription(
            SetGhostTombstoneInscriptionPayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            if (!(player.level()
                    .getBlockEntity(payload.pos())
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
             * 保存刻字。
             */
            blockEntity.setInscription(
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

    public static void sendSetInscription(
            BlockPos pos,
            String inscription
    ) {

        PacketDistributor.sendToServer(
                new SetGhostTombstoneInscriptionPayload(
                        pos,
                        inscription
                )
        );
    }
}