package com.qidate.qisplan2.network.possession;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.GhostPossessionClientState;
import com.qidate.qisplan2.client.screen.GhostPossessionScreen;
import com.qidate.qisplan2.ghost.possession.manager.GhostPossessionManager;
import com.qidate.qisplan2.ghost.possession.manager.GhostPossessionSession;
import com.qidate.qisplan2.ghost.possession.manager.GhostSuppressionAllocationHandler;
import com.qidate.qisplan2.network.payload.*;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class GhostPossessionNetwork {

    private GhostPossessionNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");

        /*
         * ========================================================
         * C2S：玩家输入
         * ========================================================
         */

        registrar.playToServer(
                GhostPossessionInputPayload.TYPE,
                GhostPossessionInputPayload.STREAM_CODEC,
                GhostPossessionNetwork::handleInput
        );

        /*
         * ========================================================
         * S2C：开始
         * ========================================================
         */

        registrar.playToClient(
                GhostPossessionStartPayload.TYPE,
                GhostPossessionStartPayload.STREAM_CODEC,
                GhostPossessionNetwork::handleStart
        );

        /*
         * ========================================================
         * S2C：更新
         * ========================================================
         */

        registrar.playToClient(
                GhostPossessionUpdatePayload.TYPE,
                GhostPossessionUpdatePayload.STREAM_CODEC,
                GhostPossessionNetwork::handleUpdate
        );

        /*
         * ========================================================
         * S2C：结束
         * ========================================================
         */

        registrar.playToClient(
                GhostPossessionEndPayload.TYPE,
                GhostPossessionEndPayload.STREAM_CODEC,
                GhostPossessionNetwork::handleEnd
        );

        /*
         * ========================================================
         * C2S：灵异配平
         * ========================================================
         */

        registrar.playToServer(
                GhostSuppressionAllocationPayload.TYPE,
                GhostSuppressionAllocationPayload.STREAM_CODEC,
                GhostPossessionNetwork::handleSuppressionAllocation
        );
    }

    /*
     * ========================================================
     * C2S：玩家输入
     * ========================================================
     */

    private static void handleInput(
            GhostPossessionInputPayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            var session =
                    GhostPossessionManager.get(
                            player
                    );

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

    private static void handleStart(
            GhostPossessionStartPayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            GhostPossessionClientState.start(
                    payload.totalTicks()
            );

            Minecraft.getInstance().setScreen(
                    new GhostPossessionScreen()
            );

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 开始驾驭小游戏：总时间={} tick",
                    payload.totalTicks()
            );
        });
    }

    /*
     * ========================================================
     * S2C：更新
     * ========================================================
     */

    private static void handleUpdate(
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

    private static void handleEnd(
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

    /*
     * ========================================================
     * C2S：灵异配平
     * ========================================================
     */

    private static void handleSuppressionAllocation(
            GhostSuppressionAllocationPayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            boolean success =
                    GhostSuppressionAllocationHandler.allocate(
                            player,
                            payload.sourceGhost(),
                            payload.targetGhost(),
                            payload.slotIndex(),
                            payload.x(),
                            payload.y()
                    );

            QisPlan2.LOGGER.info(
                    "[灵异配平] 分配请求：{} 的 slot {} → {}，结果={}",
                    payload.sourceGhost(),
                    payload.slotIndex(),
                    payload.targetGhost(),
                    success ? "成功" : "失败"
            );
        });
    }

    /*
     * ========================================================
     * S2C：开始
     * ========================================================
     */

    public static void sendStart(
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

    /*
     * ========================================================
     * S2C：更新
     * ========================================================
     */

    public static void sendUpdate(
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

    /*
     * ========================================================
     * S2C：结束
     * ========================================================
     */

    public static void sendEnd(
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

    /*
     * ========================================================
     * C2S：输入
     * ========================================================
     */

    public static void sendInput(
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

    /*
     * ========================================================
     * C2S：灵异配平
     * ========================================================
     */

    public static void sendSuppressionAllocation(
            ResourceLocation sourceGhost,
            ResourceLocation targetGhost,
            int slotIndex,
            double x,
            double y
    ) {

        PacketDistributor.sendToServer(
                new GhostSuppressionAllocationPayload(
                        sourceGhost,
                        targetGhost,
                        slotIndex,
                        x,
                        y
                )
        );
    }
}