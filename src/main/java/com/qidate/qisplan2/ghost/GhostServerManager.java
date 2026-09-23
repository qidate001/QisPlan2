package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import com.qidate.qisplan2.ghost.domain.GhostDomainServerManager;
import com.qidate.qisplan2.ghost.possession.manager.GhostPossessionManager;
import com.qidate.qisplan2.ghost.reboot.GhostRebootManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class GhostServerManager {

    private GhostServerManager() {}

    public static void tick(
            MinecraftServer server
    ) {

        /*
         * 驭鬼系统
         */
        GhostPossessionManager.tick(server);

        /*
         * 鬼域
         */
        GhostDomainServerManager.tick(server);

        /*
         * 每个维度鬼域
         */
        for (ServerLevel level : server.getAllLevels()) {
            GhostDomainManager.get(level).tick();
        }

        /*
         * 每个玩家时间记录
         */
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            GhostRebootManager.tick(player);
        }
    }
}