package com.qidate.qisplan2.network.curse;

import com.qidate.qisplan2.client.curse.ClientCurseState;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.network.payload.SyncCursePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public final class CurseNetwork {

    private CurseNetwork() {
    }


    /*
     * ========================================================
     * 注册网络包
     * ========================================================
     */

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");


        /*
         * ========================================================
         * S2C：同步诅咒
         * ========================================================
         */

        registrar.playToClient(
                SyncCursePayload.TYPE,
                SyncCursePayload.STREAM_CODEC,
                CurseNetwork::handleSync
        );
    }


    /*
     * ========================================================
     * S2C：客户端处理
     * ========================================================
     */

    private static void handleSync(
            SyncCursePayload payload,
            IPayloadContext context
    ) {

        context.enqueueWork(() -> {

            /*
             * 客户端只保存服务器同步过来的展示数据。
             */
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
    }


    /*
     * ========================================================
     * S2C：向玩家同步当前全部诅咒
     * ========================================================
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
                            getStrength(curse),
                            getRemainingTicks(curse)
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


    /*
     * ========================================================
     * 获取展示数据
     * ========================================================
     */

    /**
     * 获取诅咒强度。
     *
     * <p>
     * 当前只有部分 Curse 拥有强度，
     * 没有强度数据的诅咒暂时显示 0。
     * </p>
     */
    private static int getStrength(
            Curse curse
    ) {

        if (curse instanceof
                com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurse lifeCurse
        ) {

            return lifeCurse.getStrength();
        }

        return 0;
    }


    /**
     * 获取诅咒剩余时间。
     *
     * <p>
     * 当前只有部分 Curse 拥有剩余时间，
     * 永久诅咒暂时显示 0。
     * </p>
     */
    private static int getRemainingTicks(
            Curse curse
    ) {

        if (curse instanceof
                com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurse lifeCurse
        ) {

            return lifeCurse.getRemainingTicks();
        }

        return 0;
    }
}