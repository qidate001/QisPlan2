package com.qidate.qisplan2.network;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.network.payload.DoorGhostMarkPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public final class DoorGhostMarkNetwork {

    private DoorGhostMarkNetwork() {
    }

    public static void sendMark(
            ServerPlayer player,
            UUID target,
            boolean marked
    ) {

        Entity entity =
                player.serverLevel().getEntity(
                        target
                );

        if (entity == null) {
            return;
        }

        QisPlan2.LOGGER.info(
                "[QisPlan2] 发送门鬼标记：玩家={}，实体ID={}，marked={}",
                player.getName().getString(),
                entity.getId(),
                marked
        );

        PacketDistributor.sendToPlayer(
                player,
                new DoorGhostMarkPayload(
                        entity.getId(),
                        marked
                )
        );
    }
}