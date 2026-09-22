package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class GhostDomainLayerHandler {

    private GhostDomainLayerHandler() {
    }

    public static boolean raise(
            ServerPlayer player
    ) {
        return changeLayer(player, 1);
    }

    public static boolean lower(
            ServerPlayer player
    ) {
        return changeLayer(player, -1);
    }

    private static boolean changeLayer(
            ServerPlayer player,
            int delta
    ) {

        Entity target =
                GhostDomainTargetHandler.getLookTarget(player);

        if (target == null) {
            return false;
        }

        ServerLevel level =
                player.serverLevel();

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        GhostDomain domain =
                manager.getEffectiveDomain(target);

        if (domain == null) {
            return false;
        }

        int currentLayer =
                GhostLayerHandler.getLayer(target);

        int newLayer =
                currentLayer + delta;

        int maxLayer =
                domain.getLayer();

        if (newLayer < 1) {
            newLayer = 1;
        }

        if (newLayer > maxLayer) {
            newLayer = maxLayer;
        }

        if (newLayer == currentLayer) {
            return false;
        }

        GhostLayerHandler.setLayer(
                target,
                newLayer
        );

        QisPlan2.LOGGER.info(
                "[鬼域层数] {} 将 {} 的层数从 {} 调整为 {}",
                player.getGameProfile().getName(),
                target.getName().getString(),
                currentLayer,
                newLayer
        );

        return true;
    }
}