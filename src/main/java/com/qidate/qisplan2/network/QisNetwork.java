package com.qidate.qisplan2.network;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.entity.GhostDoorPlateBlockEntity;
import com.qidate.qisplan2.client.DoorGhostMarkClient;
import com.qidate.qisplan2.client.GhostPianoMusicClient;
import com.qidate.qisplan2.client.GhostPossessionClientState;
import com.qidate.qisplan2.ghost.domain.*;
import com.qidate.qisplan2.ghost.reboot.GhostRebootManager;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
import com.qidate.qisplan2.client.screen.GhostPossessionScreen;
import com.qidate.qisplan2.client.screen.GhostDoorPlateScreen;
import com.qidate.qisplan2.core.ModItems;
import com.qidate.qisplan2.ghost.GhostPossessionSession;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.divinationslip.GhostDivinationSlipAbility;
import com.qidate.qisplan2.ghost.ability.doorghost.DoorGhostAbilityHandler;
import com.qidate.qisplan2.ghost.ability.ghosteye.GhostEyeAbility;
import com.qidate.qisplan2.ghost.domain.type.eye.GhostEyeDomainController;
import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import com.qidate.qisplan2.ghost.reboot.GhostRebootSources;
import com.qidate.qisplan2.network.payload.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

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

        /*
         * ========================================================
         * 鬼域
         * ========================================================
         */

        registrar.playToClient(
                GhostDomainAddPayload.TYPE,
                GhostDomainAddPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        ClientGhostDomainManager.add(
                                payload.id(),
                                payload.sourceUUID(),
                                payload.domainType(),
                                payload.dimension(),
                                payload.x(),
                                payload.y(),
                                payload.z(),
                                payload.strength(),
                                payload.layer(),
                                payload.shapeType(),
                                payload.radius()
                        );

//                        QisPlan2.LOGGER.info(
//                                "[GhostDomain] CLIENT ADD: id={} type={} pos=({}, {}, {}) radius={} strength={} layer={}",
//                                payload.id(),
//                                payload.domainType(),
//                                payload.x(),
//                                payload.y(),
//                                payload.z(),
//                                payload.radius(),
//                                payload.strength(),
//                                payload.layer()
//                        );
                    });
                }
        );

        registrar.playToClient(
                GhostDomainRemovePayload.TYPE,
                GhostDomainRemovePayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        ClientGhostDomainManager.remove(
                                payload.id()
                        );

//                        QisPlan2.LOGGER.info(
//                                "[GhostDomain] CLIENT REMOVE: id={}",
//                                payload.id()
//                        );
                    });
                }
        );

        registrar.playToClient(
                GhostDomainUpdatePayload.TYPE,
                GhostDomainUpdatePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {

                        ClientGhostDomainManager.update(
                                payload.id(),
                                payload.x(),
                                payload.y(),
                                payload.z(),
                                payload.strength(),
                                payload.layer(),
                                payload.radius(),
                                payload.immediate()
                        );

//                QisPlan2.LOGGER.info(
//                        "[GhostDomain] CLIENT UPDATE: id={} pos=({}, {}, {}) strength={} layer={} radius={}",
//                        payload.id(),
//                        payload.x(),
//                        payload.y(),
//                        payload.z(),
//                        payload.strength(),
//                        payload.layer(),
//                        payload.radius()
//                );
                    });
                }
        );


        // 化虹
        registrar.playToServer(
                GhostDomainTeleportPayload.TYPE,
                GhostDomainTeleportPayload.STREAM_CODEC,
                QisNetwork::handleGhostDomainTeleport
        );

        // 为实体提升层数
        registrar.playToServer(
                GhostDomainRaiseLayerPayload.TYPE,
                GhostDomainRaiseLayerPayload.STREAM_CODEC,
                QisNetwork::handleGhostDomainRaiseLayer
        );

        // 为实体降低层数
        registrar.playToServer(
                GhostDomainLowerLayerPayload.TYPE,
                GhostDomainLowerLayerPayload.STREAM_CODEC,
                QisNetwork::handleGhostDomainLowerLayer
        );

        /*
         * ========================================================
         * 鬼签
         * ========================================================
         */

        registrar.playToServer(
                GhostDivinationUsePayload.TYPE,
                GhostDivinationUsePayload.STREAM_CODEC,
                QisNetwork::handleGhostDivinationUse
        );

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

    /*
     * ========================================================
     * S2C：鬼域
     * ========================================================
     */
    public static void sendGhostDomainAdd(
            ServerLevel level,
            GhostDomain domain
    ) {
        PacketDistributor.sendToPlayersInDimension(
                level,
                GhostDomainAddPayload.from(domain)
        );
    }
    public static void sendGhostDomainAdd(
            ServerPlayer player,
            GhostDomain domain
    ) {
        PacketDistributor.sendToPlayer(
                player,
                GhostDomainAddPayload.from(domain)
        );
    }

    public static void sendGhostDomainRemove(
            ServerLevel level,
            UUID domainId
    ) {
        PacketDistributor.sendToPlayersInDimension(
                level,
                new GhostDomainRemovePayload(
                        domainId
                )
        );
    }

    public static void sendGhostDomainUpdate(
            ServerLevel level,
            GhostDomain domain,
            boolean immediate
    ) {
        PacketDistributor.sendToPlayersInDimension(
                level,
                new GhostDomainUpdatePayload(
                        domain.getId(),

                        domain.getX(),
                        domain.getY(),
                        domain.getZ(),

                        domain.getStrength(),
                        domain.getLayer(),
                        domain.getRadius(),

                        immediate
                )
        );
    }

    private static void handleGhostDomainTeleport(
            GhostDomainTeleportPayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            GhostDomainTeleportHandler.teleport(
                    player
            );
        });
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

    public static void sendGhostDomainTeleport() {

        PacketDistributor.sendToServer(
                new GhostDomainTeleportPayload()
        );
    }

    public static void sendGhostDomainRaiseLayer() {
        PacketDistributor.sendToServer(
                new GhostDomainRaiseLayerPayload()
        );
    }

    public static void sendGhostDomainLowerLayer() {
        PacketDistributor.sendToServer(
                new GhostDomainLowerLayerPayload()
        );
    }

    private static void handleGhostDomainRaiseLayer(
            GhostDomainRaiseLayerPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

//        QisPlan2.LOGGER.info(
//                "[鬼域层数] 收到提升层数请求：{}",
//                player.getGameProfile().getName()
//        );

        GhostDomainLayerHandler.raise(player);
    }

    private static void handleGhostDomainLowerLayer(
            GhostDomainLowerLayerPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

//        QisPlan2.LOGGER.info(
//                "[鬼域层数] 收到降低层数请求：{}",
//                player.getGameProfile().getName()
//        );

        GhostDomainLayerHandler.lower(player);
    }

    private static void changeTargetLayer(
            ServerPlayer player,
            int delta
    ) {
        Entity target =
                GhostDomainTargetHandler.getLookTarget(player);

        if (target == null) {
            return;
        }

        GhostDomainManager manager =
                GhostDomainManager.get(player.serverLevel());

        GhostDomain domain =
                manager.getEffectiveDomain(target);

        if (domain == null) {
            return;
        }

        int currentLayer =
                GhostLayerHandler.getLayer(target);

        int newLayer =
                Mth.clamp(
                        currentLayer + delta,
                        1,
                        domain.getLayer()
                );

        if (newLayer == currentLayer) {
            return;
        }

        GhostLayerHandler.setLayer(
                target,
                newLayer
        );
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

    /*
     * ========================================================
     * S2C：鬼签
     * ========================================================
     */
    public static void sendGhostDivinationResult(
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
     * C2S：鬼签使用
     * ========================================================
     */
    private static void handleGhostDivinationUse(
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
                    GhostDivinationSlipAbility.ID
            )) {

                return;
            }

            GhostDivinationSlipAbility.useResult(
                    player,
                    payload.result()
            );
        });
    }

    public static void sendGhostDivinationUse(
            int result
    ) {

        PacketDistributor.sendToServer(
                new GhostDivinationUsePayload(
                        result
                )
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