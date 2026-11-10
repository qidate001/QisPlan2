package com.qidate.qisplan2.network.divinationslip;

import com.qidate.qisplan2.ghost.module.host.player.DivinationSlipInput;
import com.qidate.qisplan2.core.ModItems;
import com.qidate.qisplan2.ghost.module.implementation.divinationslip.GhostDivinationSlipModule;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.network.payload.GhostDivinationResultPayload;
import com.qidate.qisplan2.network.payload.GhostDivinationUsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class GhostDivinationNetwork {

    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostDivinationNetwork() {
    }

    /**
     * 注册鬼签网络消息及对应处理器，使客户端结果输入进入服务端校验。
     */
    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");

        /*
         * ========================================================
         * C2S：鬼签使用
         * ========================================================
         */

        registrar.playToServer(
                GhostDivinationUsePayload.TYPE,
                GhostDivinationUsePayload.STREAM_CODEC,
                GhostDivinationNetwork::handleUse
        );

        /*
         * ========================================================
         * S2C：鬼签结果
         * ========================================================
         */

        registrar.playToClient(
                GhostDivinationResultPayload.TYPE,
                GhostDivinationResultPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        Minecraft minecraft =
                                Minecraft.getInstance();

                        ItemStack resultStack;

                        switch (payload.result()) {

                            case 0 -> resultStack =
                                    new ItemStack(
                                            ModItems.LIFE_SIGN.get()
                                    );

                            case 1 -> resultStack =
                                    new ItemStack(
                                            ModItems.DEATH_SIGN.get()
                                    );

                            case 2 -> resultStack =
                                    new ItemStack(
                                            ModItems.GHOST_SIGN.get()
                                    );

                            default -> {
                                return;
                            }
                        }

                        /*
                         * 原版不死图腾触发动画。
                         */
                        minecraft.gameRenderer
                                .displayItemActivation(
                                        resultStack
                                );
                    });
                }
        );
    }

    /*
     * ========================================================
     * C2S：处理鬼签使用
     * ========================================================
     */

    private static void handleUse(
            GhostDivinationUsePayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            /*
             * 必须真的驾驭了鬼签。
             */
            if (!PossessionHandler.hasGhost(
                    player,
                    GhostDivinationSlipModule.ID
            )) {

                return;
            }

            DivinationSlipInput.useResult(
                    player,
                    payload.result()
            );
        });
    }

    /*
     * ========================================================
     * S2C：发送鬼签结果
     * ========================================================
     */

    public static void sendResult(
            ServerPlayer player,
            int result
    ) {

        PacketDistributor.sendToPlayer(
                player,
                new GhostDivinationResultPayload(
                        result
                )
        );
    }

    /*
     * ========================================================
     * C2S：发送鬼签使用
     * ========================================================
     */

    public static void sendUse(
            int result
    ) {

        PacketDistributor.sendToServer(
                new GhostDivinationUsePayload(
                        result
                )
        );
    }
}