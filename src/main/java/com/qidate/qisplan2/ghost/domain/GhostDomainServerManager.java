package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.ghost.domain.type.umbrella.GhostUmbrellaDomainController;
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
             * 鬼雨伞 更新会自动创建 / 删除鬼域的控制器
             */
            for (ServerPlayer player : level.players()) {
                GhostUmbrellaDomainController.tick(player);
            }

            /*
             * 执行鬼域自身行为
             */
            GhostDomainManager.get(level).tick();
        }
    }
}