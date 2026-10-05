package com.qidate.qisplan2.network.ghostmist;

import com.qidate.qisplan2.ghost.domain.type.mist.GhostMistDomainController;
import com.qidate.qisplan2.ghost.possession.ability.ghostmist.GhostMistAbility;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.network.payload.GhostMistTogglePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class GhostMistNetwork {

    private GhostMistNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {
        var registrar =
                event.registrar("1");

        /*
         * ========================================================
         * C2S：鬼雾开启 / 关闭
         * ========================================================
         */

        registrar.playToServer(
                GhostMistTogglePayload.TYPE,
                GhostMistTogglePayload.STREAM_CODEC,
                GhostMistNetwork::handleToggle
        );
    }

    /*
     * ========================================================
     * 处理：鬼雾开启 / 关闭
     * ========================================================
     */

    private static void handleToggle(
            GhostMistTogglePayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            /*
             * 必须真正驾驭鬼雾。
             */
            if (!PossessionHandler.hasGhost(
                    player,
                    GhostMistAbility.ID
            )) {
                return;
            }

            /*
             * 开启 / 关闭鬼雾鬼域。
             */
            GhostMistDomainController.toggle(
                    player
            );
        });
    }

    /*
     * ========================================================
     * C2S：开启 / 关闭鬼雾
     * ========================================================
     */

    public static void sendToggle() {

        PacketDistributor.sendToServer(
                new GhostMistTogglePayload()
        );
    }
}