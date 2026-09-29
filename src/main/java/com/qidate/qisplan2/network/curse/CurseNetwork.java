package com.qidate.qisplan2.network.curse;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.curse.ClientCurseState;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.network.payload.SyncCursePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 诅咒系统网络同步。
 *
 * <p>
 * 负责服务端与客户端之间的诅咒展示状态同步。
 * </p>
 *
 * <p>
 * 本类不参与诅咒实际逻辑，
 * 也不依赖任何具体诅咒类型。
 * </p>
 */
public final class CurseNetwork {

    private CurseNetwork() {
    }

    /**
     * 注册诅咒网络 Payload。
     */
    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");

        registrar.playToClient(
                SyncCursePayload.TYPE,
                SyncCursePayload.STREAM_CODEC,
                CurseNetwork::handleSync
        );
    }

    /**
     * 客户端接收服务端诅咒同步。
     */
    private static void handleSync(
            SyncCursePayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            ClientCurseState.clear();

            for (SyncCursePayload.CurseData curse :
                    payload.curses()
            ) {

                ClientCurseState.put(
                        new ClientCurseState.CurseData(
                                curse.id(),
                                curse.type(),
                                curse.strength(),
                                curse.remainingTicks()
                        )
                );
            }
        });

        QisPlan2.LOGGER.info(
                "[诅咒网络] 客户端收到诅咒同步：{} 个",
                payload.curses().size()
        );
    }

    /**
     * 向指定玩家同步当前全部诅咒。
     */
    public static void sync(
            ServerPlayer player
    ) {

        List<
                SyncCursePayload.CurseData
                > curses =
                new ArrayList<>();

        for (Curse curse :
                CurseManager.getByTarget(
                        player.getUUID()
                )
        ) {

            curses.add(
                    new SyncCursePayload.CurseData(
                            curse.getId(),
                            curse.getType(),
                            0,
                            0
                    )
            );
        }

        PacketDistributor.sendToPlayer(
                player,
                new SyncCursePayload(
                        curses
                )
        );
    }
}