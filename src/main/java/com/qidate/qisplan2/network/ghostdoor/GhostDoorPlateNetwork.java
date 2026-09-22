package com.qidate.qisplan2.network.ghostdoor;

import com.qidate.qisplan2.block.entity.GhostDoorPlateBlockEntity;
import com.qidate.qisplan2.client.screen.GhostDoorPlateScreen;
import com.qidate.qisplan2.network.payload.OpenGhostDoorPlateScreenPayload;
import com.qidate.qisplan2.network.payload.SetGhostDoorPlateNumberPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class GhostDoorPlateNetwork {

    private GhostDoorPlateNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");

        /*
         * ========================================================
         * S2C：打开鬼门牌界面
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

        /*
         * ========================================================
         * C2S：设置鬼门牌数字
         * ========================================================
         */

        registrar.playToServer(
                SetGhostDoorPlateNumberPayload.TYPE,
                SetGhostDoorPlateNumberPayload.STREAM_CODEC,
                GhostDoorPlateNetwork::handleSetNumber
        );
    }

    private static void handleSetNumber(
            SetGhostDoorPlateNumberPayload payload,
            IPayloadContext context
    ) {

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

    /*
     * ========================================================
     * S2C：打开鬼门牌界面
     * ========================================================
     */

    public static void sendOpenScreen(
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

    /*
     * ========================================================
     * C2S：设置数字
     * ========================================================
     */

    public static void sendSetNumber(
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