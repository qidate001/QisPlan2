package com.qidate.qisplan2.network.ghostdomain;

import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainLayerHandler;
import com.qidate.qisplan2.ghost.domain.GhostDomainTeleportHandler;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainVisionSystem;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationRegion;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationSavedData;
import com.qidate.qisplan2.ghost.isolation.IsolationState;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationCuboid;
import com.qidate.qisplan2.ghost.isolation.client.ClientGhostIsolationManager;
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
         * S2C：灵异隔绝区域添加
         * ========================================================
         */

        registrar.playToClient(
                GhostIsolationAddPayload.TYPE,
                GhostIsolationAddPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        List<ClientGhostIsolationCuboid> cuboids =
                                new ArrayList<>(
                                        payload.cuboids().size()
                                );

                        for (GhostIsolationAddPayload.CuboidEntry entry :
                                payload.cuboids()) {

                            cuboids.add(
                                    new ClientGhostIsolationCuboid(
                                            entry.minX(),
                                            entry.minY(),
                                            entry.minZ(),
                                            entry.maxX(),
                                            entry.maxY(),
                                            entry.maxZ()
                                    )
                            );
                        }

                        ClientGhostIsolationManager.add(
                                payload.regionId(),
                                payload.dimension(),
                                cuboids
                        );
                    });
                }
        );

        /*
         * ========================================================
         * S2C：灵异隔绝区域移除
         * ========================================================
         */

        registrar.playToClient(
                GhostIsolationRemovePayload.TYPE,
                GhostIsolationRemovePayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        ClientGhostIsolationManager.remove(
                                payload.regionId()
                        );
                    });
                }
        );

        /*
         * ========================================================
         * S2C：鬼域视觉（实体的视觉状态同步，和鬼域Shader渲染无关）
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
     * S2C：灵异隔绝区域添加
     * ========================================================
     */

    /**
     * 向指定维度中的所有玩家同步一个灵异隔绝区域。
     *
     * <p>
     * 服务端确认一个 Region 成立后，
     * 可以通过这个方法将其精确空间同步到客户端。
     * </p>
     *
     * @param level Region 所在的服务端维度
     * @param region 要同步的灵异隔绝区域
     */
    public static void sendIsolationAdd(
            ServerLevel level,
            GhostIsolationRegion region
    ) {
        PacketDistributor.sendToPlayersInDimension(
                level,
                GhostIsolationAddPayload.from(
                        region
                )
        );
    }

    /**
     * 向指定玩家同步一个灵异隔绝区域。
     *
     * <p>
     * 主要用于玩家进入服务器、
     * 切换维度或者需要补发客户端缓存时。
     * </p>
     *
     * @param player 要接收数据的玩家
     * @param region 要同步的灵异隔绝区域
     */
    public static void sendIsolationAdd(
            ServerPlayer player,
            GhostIsolationRegion region
    ) {
        PacketDistributor.sendToPlayer(
                player,
                GhostIsolationAddPayload.from(
                        region
                )
        );
    }

    /*
     * ========================================================
     * S2C：鬼域视觉（实体的视觉状态同步，和鬼域Shader渲染无关）
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
     * S2C：灵异隔绝区域移除
     * ========================================================
     */

    /**
     * 向指定维度中的所有玩家移除一个灵异隔绝区域。
     *
     * <p>
     * 客户端只需要 Region UUID，
     * 无需再次同步空间数据。
     * </p>
     *
     * @param level Region 所在的服务端维度
     * @param regionId 要移除的 Region UUID
     */
    public static void sendIsolationRemove(
            ServerLevel level,
            UUID regionId
    ) {
        PacketDistributor.sendToPlayersInDimension(
                level,
                new GhostIsolationRemovePayload(
                        regionId
                )
        );
    }

    /**
     * 向指定玩家移除一个灵异隔绝区域。
     *
     * <p>
     * 主要用于玩家进入服务器、
     * 切换维度或者客户端缓存需要修正时。
     * </p>
     *
     * @param player 要接收数据的玩家
     * @param regionId 要移除的 Region UUID
     */
    public static void sendIsolationRemove(
            ServerPlayer player,
            UUID regionId
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new GhostIsolationRemovePayload(
                        regionId
                )
        );
    }

    /**
     * 将指定维度中当前已经确认隔绝的所有区域，
     * 单独同步给指定玩家。
     *
     * <p>
     * GhostIsolationSavedData 是跨维度保存的，
     * 因此这里必须根据 Region 的 dimension
     * 过滤出玩家当前所在维度。
     * </p>
     *
     * @param player 目标玩家
     */
    public static void sendAllIsolationRegions(
            ServerPlayer player
    ) {
        ServerLevel level = player.serverLevel();

        GhostIsolationSavedData data =
                GhostIsolationSavedData.get(level);

        for (GhostIsolationRegion region : data.getRegions()) {

            /*
             * SavedData 中可能保存多个维度的 Region。
             *
             * 玩家只能收到当前维度的数据。
             */
            if (!region.getDimension().equals(level.dimension())) {
                continue;
            }

            /*
             * DIRTY 表示这个 Region 已经失效，
             * 当前不能把它当成有效隔绝区域发送给客户端。
             */
            if (region.getState() != IsolationState.ISOLATED) {
                continue;
            }

            sendIsolationAdd(player, region);
        }
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