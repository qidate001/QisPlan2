package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.ghost.domain.umbrella.GhostUmbrellaDomain;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class GhostDomainServerTick {

    private GhostDomainServerTick() {}

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {

        MinecraftServer server =
                event.getServer();

        for (ServerLevel level :
                server.getAllLevels()) {

            /*
             * 负责创建 / 更新 / 删除鬼域
             */
            for (ServerPlayer player :
                    level.players()) {

                GhostUmbrellaDomain.tick(player);
            }

            /*
             * 更新实体与鬼域之间的关系
             */
            GhostDomainEntityTracker tracker =
                    GhostDomainEntityTracker.get(level);

            for (Entity entity :
                    level.getAllEntities()) {

                tracker.update(entity);
            }

            /*
             * 最后执行所有鬼域的行为
             */
            GhostDomainManager
                    .get(level)
                    .tick();
        }
    }
}