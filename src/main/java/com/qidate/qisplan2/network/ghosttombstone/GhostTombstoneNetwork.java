package com.qidate.qisplan2.network.ghosttombstone;

import com.qidate.qisplan2.block.entity.GhostTombstoneBlockEntity;
import com.qidate.qisplan2.client.screen.GhostTombstoneScreen;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.curse.CurseRegistry;
import com.qidate.qisplan2.ghost.curse.CurseType;
import com.qidate.qisplan2.ghost.curse.type.ghosttombstone.GhostTombstoneCurseSource;
import com.qidate.qisplan2.ghost.curse.type.ghosttombstone.GhostTombstoneCurseType;
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
             * ========================================================
             * 先移除这块墓碑原本产生的诅咒。
             * ========================================================
             *
             * 无论新刻的是：
             *
             * 1. 原来的名字
             * 2. 另一个玩家的名字
             * 3. 空白
             * 4. 无效名字
             *
             * 都先清理旧诅咒。
             *
             * 这样可以保证：
             *
             * 一块墓碑最多只有一个诅咒。
             */
            CurseManager.removeBySource(
                    curse -> {

                        if (!(curse.getSource()
                                instanceof GhostTombstoneCurseSource source)) {

                            return false;
                        }

                        return source.dimension().equals(
                                player.level().dimension()
                        )
                                && source.pos().equals(
                                payload.pos()
                        );
                    }
            );

            /*
             * ========================================================
             * 保存新的刻字。
             * ========================================================
             */
            blockEntity.setInscription(
                    payload.inscription()
            );

            /*
             * ========================================================
             * 根据新的名字创建诅咒。
             * ========================================================
             *
             * 只有当刻下的名字对应一个在线玩家时，
             * 才会产生新的鬼墓碑诅咒。
             */
            ServerPlayer target =
                    player.server
                            .getPlayerList()
                            .getPlayerByName(
                                    payload.inscription()
                            );

            if (target == null) {
                return;
            }

            CurseType curseType =
                    CurseRegistry.get(
                            GhostTombstoneCurseType.ID
                    );

            if (curseType == null) {
                return;
            }

            GhostTombstoneCurseSource source =
                    new GhostTombstoneCurseSource(
                            player.level().dimension(),
                            payload.pos()
                    );

            Curse curse =
                    curseType.create(
                            target.getUUID(),
                            source
                    );

            CurseManager.add(curse);
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