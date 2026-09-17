package com.qidate.qisplan2.ghost.domain.type.debug;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.domain.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class DebugDomainController {

    public static final ResourceLocation DOMAIN_TYPE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "debug_domain"
            );

    private DebugDomainController() {
    }

    public static void create(
            ServerPlayer player,
            double radius,
            int layer,
            double strength
    ) {

        GhostDomainManager manager =
                GhostDomainManager.get(
                        player.serverLevel()
                );

        manager.removeBySourceAndType(
                player.getUUID(),
                DOMAIN_TYPE
        );

        manager.add(
                new GhostDomain(
                        UUID.randomUUID(),
                        player.getUUID(),
                        DOMAIN_TYPE,
                        strength,
                        layer,
                        radius,
                        player.serverLevel().dimension(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        new CylinderDomainShape(radius),
                        GhostDomainUpdateMode.MANUAL,
                        0.0D,
                        new DebugDomainBehavior()
                )
        );
    }

    public static void remove(
            ServerPlayer player
    ) {

        GhostDomainManager.get(
                player.serverLevel()
        ).removeBySource(
                player.getUUID()
        );
    }
}