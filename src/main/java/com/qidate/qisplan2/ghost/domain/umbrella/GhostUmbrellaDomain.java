package com.qidate.qisplan2.ghost.domain.umbrella;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.domain.CylinderDomainShape;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class GhostUmbrellaDomain {

    public static final double DOMAIN_RADIUS = 50.0D;

    public static final ResourceLocation TYPE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_umbrella"
            );

    private GhostUmbrellaDomain() {
    }

    public static void ensure(
            ServerPlayer player
    ) {

        ServerLevel level =
                player.serverLevel();

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        UUID playerUUID =
                player.getUUID();

        GhostDomain domain =
                manager.getBySource(playerUUID);

        if (domain == null) {

            domain =
                    new GhostDomain(
                            UUID.randomUUID(),
                            playerUUID,
                            TYPE,
                            level.dimension(),
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            new CylinderDomainShape(
                                    DOMAIN_RADIUS
                            )
                    );

            manager.add(domain);

            return;
        }

        /*
         * 鬼域跟随撑伞者移动
         */
        domain.setPosition(
                player.getX(),
                player.getY(),
                player.getZ()
        );
    }

    public static void remove(
            ServerPlayer player
    ) {

        GhostDomainManager
                .get(player.serverLevel())
                .removeBySource(
                        player.getUUID()
                );
    }
}