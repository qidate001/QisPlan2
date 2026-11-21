package com.qidate.qisplan2.ghost.domain;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class GhostDomainServerManager {

    private GhostDomainServerManager() {}

    public static void tick(
            MinecraftServer server
    ) {

        for (ServerLevel level : server.getAllLevels()) {
            /*
             * 执行鬼域自身行为
             */
            GhostDomainManager.get(level).tick();
        }
    }
}