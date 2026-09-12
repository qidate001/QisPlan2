package com.qidate.qisplan2.ghost.domain.debug;

import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class DebugDomainBehavior
        implements GhostDomainBehavior {

    @Override
    public void tick(
            ServerLevel level,
            GhostDomain domain
    ) {

        for (ServerPlayer player : level.players()) {

            if (!domain.contains(
                    player.getX(),
                    player.getY(),
                    player.getZ()
            )) {
                continue;
            }

            player.displayClientMessage(
                    Component.literal(
                            "§5【DEBUG】当前身处调试鬼域中"
                    ),
                    true
            );
        }
    }
}