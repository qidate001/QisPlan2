package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.GhostServerManager;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.possession.ability.ghostdoor.knocking.KnockingGhostDoorSystem;
import com.qidate.qisplan2.ghost.tombstone.GhostTombstoneInscriptionSystem;
import com.qidate.qisplan2.network.curse.CurseNetwork;
import com.qidate.qisplan2.structure.GhostGraveyardGenerationManager;
import com.qidate.qisplan2.structure.GhostLakeGenerationManager;
import com.qidate.qisplan2.structure.GhostManorGenerationManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = QisPlan2.MODID)
public final class GhostServerTickHandler {

    /**
     * 诅咒客户端校准间隔。
     *
     * <p>
     * 100 Tick = 5 秒。
     * </p>
     */
    private static final int CURSE_SYNC_INTERVAL = 100;

    /**
     * 当前距离下一次诅咒校准已经经过的 Tick。
     */
    private static int curseSyncTicks = 0;

    private GhostServerTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        var server = event.getServer();

        // ==============================
        // 各个系统各自的 Tick
        // ==============================

        // 鬼域 & 驭鬼者
        GhostServerManager.tick(
                event.getServer()
        );

        // 诅咒系统
        CurseManager.tick(
                event.getServer()
        );

        // 敲门鬼系统
        KnockingGhostDoorSystem.tick();

        // 鬼墓碑系统
        GhostTombstoneInscriptionSystem.tick(server);

        // ==============================
        // 诅咒客户端定期校准
        // ==============================

        curseSyncTicks++;

        if (curseSyncTicks >= CURSE_SYNC_INTERVAL) {
            curseSyncTicks = 0;

            for (var player :
                    server.getPlayerList().getPlayers()
            ) {
                CurseNetwork.sync(player);
            }
        }



        /*
         * 鬼庄园生成任务。
         */
        GhostManorGenerationManager.tick();

        /*
         * 鬼湖生成任务。
         */
        GhostLakeGenerationManager.tick();

        /*
         * 鬼坟场生成任务。
         */
        GhostGraveyardGenerationManager.tick();
    }
}