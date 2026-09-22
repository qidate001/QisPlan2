package com.qidate.qisplan2.network.ghosteye;

import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.ghosteye.GhostEyeAbility;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import com.qidate.qisplan2.ghost.domain.type.eye.GhostEyeDomainController;
import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import com.qidate.qisplan2.ghost.reboot.GhostRebootManager;
import com.qidate.qisplan2.ghost.reboot.GhostRebootSources;
import com.qidate.qisplan2.network.payload.GhostEyeLayerChangePayload;
import com.qidate.qisplan2.network.payload.GhostEyeRebootPayload;
import com.qidate.qisplan2.network.payload.GhostEyeSelfLayerChangePayload;
import com.qidate.qisplan2.network.payload.GhostEyeTogglePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class GhostEyeNetwork {

    private GhostEyeNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");

        /*
         * ========================================================
         * C2S：鬼眼层数调整
         * ========================================================
         */

        registrar.playToServer(
                GhostEyeLayerChangePayload.TYPE,
                GhostEyeLayerChangePayload.STREAM_CODEC,
                GhostEyeNetwork::handleLayerChange
        );

        /*
         * ========================================================
         * C2S：鬼眼开启 / 关闭
         * ========================================================
         */

        registrar.playToServer(
                GhostEyeTogglePayload.TYPE,
                GhostEyeTogglePayload.STREAM_CODEC,
                GhostEyeNetwork::handleToggle
        );

        /*
         * ========================================================
         * C2S：自身鬼域层数调整
         * ========================================================
         */

        registrar.playToServer(
                GhostEyeSelfLayerChangePayload.TYPE,
                GhostEyeSelfLayerChangePayload.STREAM_CODEC,
                GhostEyeNetwork::handleSelfLayerChange
        );

        /*
         * ========================================================
         * C2S：重启
         * ========================================================
         */

        registrar.playToServer(
                GhostEyeRebootPayload.TYPE,
                GhostEyeRebootPayload.STREAM_CODEC,
                GhostEyeNetwork::handleReboot
        );
    }

    /*
     * ========================================================
     * 处理：鬼眼层数调整
     * ========================================================
     */

    private static void handleLayerChange(
            GhostEyeLayerChangePayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            GhostDomainManager manager =
                    GhostDomainManager.get(
                            player.serverLevel()
                    );

            GhostDomain domain =
                    manager.getBySourceAndType(
                            player.getUUID(),
                            GhostEyeAbility.ID
                    );

            if (domain == null) {
                return;
            }

            int newLayer =
                    Mth.clamp(
                            domain.getLayer()
                                    + payload.delta(),
                            1,
                            6
                    );

            if (newLayer == domain.getLayer()) {
                return;
            }

            manager.updateLayer(
                    domain.getId(),
                    newLayer
            );

            GhostLayerHandler.setLayer(
                    player,
                    newLayer
            );
        });
    }

    /*
     * ========================================================
     * 处理：鬼眼开启 / 关闭
     * ========================================================
     */

    private static void handleToggle(
            GhostEyeTogglePayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            if (GhostEyeDomainController.isOpen(player)) {

                GhostEyeDomainController.close(player);

            } else {

                GhostEyeDomainController.open(player);
            }
        });
    }

    /*
     * ========================================================
     * 处理：自身鬼域层数调整
     * ========================================================
     */

    private static void handleSelfLayerChange(
            GhostEyeSelfLayerChangePayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            /*
             * 必须真正驾驭鬼眼。
             */
            if (!PossessionHandler.hasGhost(
                    player,
                    GhostEyeAbility.ID
            )) {

                return;
            }

            GhostDomain domain =
                    GhostDomainManager.get(
                            player.serverLevel()
                    ).getBySourceAndType(
                            player.getUUID(),
                            GhostEyeDomainController.DOMAIN_TYPE
                    );

            if (domain == null) {
                return;
            }

            int currentLayer =
                    GhostLayerHandler.getLayer(
                            player
                    );

            int newLayer =
                    Mth.clamp(
                            currentLayer + payload.delta(),
                            1,
                            domain.getLayer()
                    );

            if (newLayer == currentLayer) {
                return;
            }

            GhostLayerHandler.setLayer(
                    player,
                    newLayer
            );
        });
    }

    /*
     * ========================================================
     * 处理：重启
     * ========================================================
     */

    private static void handleReboot(
            GhostEyeRebootPayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            /*
             * 必须真正驾驭鬼眼。
             */
            if (!PossessionHandler.hasGhost(
                    player,
                    GhostEyeAbility.ID
            )) {

                return;
            }

            /*
             * 鬼眼必须处于开启状态。
             */
            if (!GhostEyeDomainController.isOpen(player)) {

                return;
            }

            GhostDomain domain =
                    GhostDomainManager.get(
                            player.serverLevel()
                    ).getBySourceAndType(
                            player.getUUID(),
                            GhostEyeDomainController.DOMAIN_TYPE
                    );

            if (domain == null) {
                return;
            }

            /*
             * 必须开启六层鬼域。
             */
            if (domain.getLayer() < 6) {
                return;
            }

            /*
             * 切换持续重启状态。
             */
            GhostRebootManager.toggleContinuousReboot(
                    player,
                    GhostRebootSources.ghost(
                            player.getUUID(),
                            GhostEyeAbility.ID
                    )
            );
        });
    }

    /*
     * ========================================================
     * C2S：鬼眼层数调整
     * ========================================================
     */

    public static void sendLayerChange(
            int delta
    ) {

        PacketDistributor.sendToServer(
                new GhostEyeLayerChangePayload(
                        delta
                )
        );
    }

    /*
     * ========================================================
     * C2S：重启
     * ========================================================
     */

    public static void sendReboot() {

        PacketDistributor.sendToServer(
                new GhostEyeRebootPayload()
        );
    }

    /*
     * ========================================================
     * C2S：开启 / 关闭鬼眼
     * ========================================================
     */

    public static void sendToggle() {

        PacketDistributor.sendToServer(
                new GhostEyeTogglePayload()
        );
    }

    /*
     * ========================================================
     * C2S：自身鬼域层数调整
     * ========================================================
     */

    public static void sendSelfLayerChange(
            int delta
    ) {

        PacketDistributor.sendToServer(
                new GhostEyeSelfLayerChangePayload(
                        delta
                )
        );
    }
}