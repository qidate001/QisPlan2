package com.qidate.qisplan2.network.ghostdomain;

import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainLayerHandler;
import com.qidate.qisplan2.ghost.domain.GhostDomainTeleportHandler;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainVisionSystem;
import com.qidate.qisplan2.network.payload.*;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

public final class GhostDomainNetwork {

    private GhostDomainNetwork() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");

        /*
         * ========================================================
         * S2C：鬼域添加
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
                    });
                }
        );

        /*
         * ========================================================
         * S2C：鬼域移除
         * ========================================================
         */

        registrar.playToClient(
                GhostDomainRemovePayload.TYPE,
                GhostDomainRemovePayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        ClientGhostDomainManager.remove(
                                payload.id()
                        );
                    });
                }
        );

        /*
         * ========================================================
         * S2C：鬼域更新
         * ========================================================
         */

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
                    });
                }
        );

        /*
         * ========================================================
         * S2C：鬼域视觉
         * ========================================================
         */

        registrar.playToClient(
                GhostDomainVisionPayload.TYPE,
                GhostDomainVisionPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        Map<UUID, Integer> entities =
                                new LinkedHashMap<>();

                        for (GhostDomainVisionPayload.VisionEntry entry :
                                payload.entries()) {

                            entities.put(
                                    entry.entityUUID(),
                                    entry.color()
                            );
                        }

                        ClientGhostDomainVisionSystem.update(
                                entities
                        );
                    });
                }
        );

        /*
         * ========================================================
         * C2S：化虹
         * ========================================================
         */

        registrar.playToServer(
                GhostDomainTeleportPayload.TYPE,
                GhostDomainTeleportPayload.STREAM_CODEC,
                GhostDomainNetwork::handleTeleport
        );

        /*
         * ========================================================
         * C2S：实体提升层数
         * ========================================================
         */

        registrar.playToServer(
                GhostDomainRaiseLayerPayload.TYPE,
                GhostDomainRaiseLayerPayload.STREAM_CODEC,
                GhostDomainNetwork::handleRaiseLayer
        );

        /*
         * ========================================================
         * C2S：实体降低层数
         * ========================================================
         */

        registrar.playToServer(
                GhostDomainLowerLayerPayload.TYPE,
                GhostDomainLowerLayerPayload.STREAM_CODEC,
                GhostDomainNetwork::handleLowerLayer
        );
    }

    /*
     * ========================================================
     * S2C：鬼域添加
     * ========================================================
     */

    public static void sendAdd(
            ServerLevel level,
            GhostDomain domain
    ) {
        PacketDistributor.sendToPlayersInDimension(
                level,
                GhostDomainAddPayload.from(domain)
        );
    }

    public static void sendAdd(
            ServerPlayer player,
            GhostDomain domain
    ) {
        PacketDistributor.sendToPlayer(
                player,
                GhostDomainAddPayload.from(domain)
        );
    }

    /*
     * ========================================================
     * S2C：鬼域移除
     * ========================================================
     */

    public static void sendRemove(
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

    /*
     * ========================================================
     * S2C：鬼域更新
     * ========================================================
     */

    public static void sendUpdate(
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

    /*
     * ========================================================
     * S2C：鬼域视觉
     * ========================================================
     */

    public static void sendVision(
            ServerPlayer player,
            GhostDomain domain,
            Map<UUID, Integer> visibleEntities
    ) {

        List<GhostDomainVisionPayload.VisionEntry> entries =
                new ArrayList<>(
                        visibleEntities.size()
                );

        for (Map.Entry<UUID, Integer> entry :
                visibleEntities.entrySet()) {

            entries.add(
                    new GhostDomainVisionPayload.VisionEntry(
                            entry.getKey(),
                            entry.getValue()
                    )
            );
        }

        PacketDistributor.sendToPlayer(
                player,
                new GhostDomainVisionPayload(
                        domain.getId(),
                        entries
                )
        );
    }

    /*
     * ========================================================
     * C2S：化虹
     * ========================================================
     */

    private static void handleTeleport(
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

    /*
     * ========================================================
     * C2S：实体提升层数
     * ========================================================
     */

    private static void handleRaiseLayer(
            GhostDomainRaiseLayerPayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            GhostDomainLayerHandler.raise(
                    player
            );
        });
    }

    /*
     * ========================================================
     * C2S：实体降低层数
     * ========================================================
     */

    private static void handleLowerLayer(
            GhostDomainLowerLayerPayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            GhostDomainLayerHandler.lower(
                    player
            );
        });
    }

    /*
     * ========================================================
     * 客户端：化虹
     * ========================================================
     */

    public static void sendTeleport() {

        PacketDistributor.sendToServer(
                new GhostDomainTeleportPayload()
        );
    }

    /*
     * ========================================================
     * 客户端：实体提升层数
     * ========================================================
     */

    public static void sendRaiseLayer() {

        PacketDistributor.sendToServer(
                new GhostDomainRaiseLayerPayload()
        );
    }

    /*
     * ========================================================
     * 客户端：实体降低层数
     * ========================================================
     */

    public static void sendLowerLayer() {

        PacketDistributor.sendToServer(
                new GhostDomainLowerLayerPayload()
        );
    }
}