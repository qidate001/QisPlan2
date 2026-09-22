package com.qidate.qisplan2.network;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.domain.*;
import com.qidate.qisplan2.ghost.reboot.GhostRebootManager;
import com.qidate.qisplan2.core.ModItems;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.divinationslip.GhostDivinationSlipAbility;
import com.qidate.qisplan2.network.divinationslip.GhostDivinationNetwork;
import com.qidate.qisplan2.network.ghostdoor.GhostDoorNetwork;
import com.qidate.qisplan2.ghost.ability.ghosteye.GhostEyeAbility;
import com.qidate.qisplan2.ghost.domain.type.eye.GhostEyeDomainController;
import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import com.qidate.qisplan2.ghost.reboot.GhostRebootSources;
import com.qidate.qisplan2.network.ghostdomain.GhostDomainNetwork;
import com.qidate.qisplan2.network.ghostdoor.GhostDoorPlateNetwork;
import com.qidate.qisplan2.network.ghostpiano.GhostPianoNetwork;
import com.qidate.qisplan2.network.payload.*;
import com.qidate.qisplan2.network.possession.GhostPossessionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
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
         * ========================================================
         * 鬼钢琴
         * ========================================================
         */

        GhostPianoNetwork.register(event);

        /*
         * ========================================================
         * 驾驭小游戏
         * ========================================================
         */

        GhostPossessionNetwork.register(event);

        /*
         * ========================================================
         * 鬼门
         * ========================================================
         */

        GhostDoorNetwork.register(event);

        /*
         * ========================================================
         * 鬼门牌
         * ========================================================
         */

        GhostDoorPlateNetwork.register(event);

        /*
         * ========================================================
         * 鬼域
         * ========================================================
         */

        GhostDomainNetwork.register(event);

        /*
         * ========================================================
         * 鬼签
         * ========================================================
         */

        GhostDivinationNetwork.register(event);

        /*
         * ========================================================
         * 鬼眼
         * ========================================================
         */

        registrar.playToServer(
                GhostEyeLayerChangePayload.TYPE,
                GhostEyeLayerChangePayload.STREAM_CODEC,
                (payload, context) -> {
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
        );

        registrar.playToServer(
                GhostEyeTogglePayload.TYPE,
                GhostEyeTogglePayload.STREAM_CODEC,
                (payload, context) -> {

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
        );

        registrar.playToServer(
                GhostEyeSelfLayerChangePayload.TYPE,
                GhostEyeSelfLayerChangePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {

                        if (!(context.player()
                                instanceof ServerPlayer player)) {
                            return;
                        }

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
        );

        // 重启
        registrar.playToServer(
                GhostEyeRebootPayload.TYPE,
                GhostEyeRebootPayload.STREAM_CODEC,
                QisNetwork::handleGhostEyeReboot
        );
    }

    private static void handleGhostEyeReboot(
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
             *
             * 第一次按键：
             * 开始持续重启。
             *
             * 第二次按键：
             * 停止持续重启。
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
     * C2S：鬼眼
     * ========================================================
     */
    public static void sendGhostEyeLayerChange(
            int delta
    ) {
        PacketDistributor.sendToServer(
                new GhostEyeLayerChangePayload(
                        delta
                )
        );
    }

    public static void sendGhostEyeReboot() {

        PacketDistributor.sendToServer(
                new GhostEyeRebootPayload()
        );
    }

    public static void sendGhostEyeToggle() {
        PacketDistributor.sendToServer(
                new GhostEyeTogglePayload()
        );
    }

    public static void sendGhostEyeSelfLayerChange(
            int delta
    ) {

        PacketDistributor.sendToServer(
                new GhostEyeSelfLayerChangePayload(
                        delta
                )
        );
    }
}