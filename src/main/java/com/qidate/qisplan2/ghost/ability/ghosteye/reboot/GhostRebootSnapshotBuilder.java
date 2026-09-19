package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class GhostRebootSnapshotBuilder {

    private GhostRebootSnapshotBuilder() {
    }

    public static GhostRebootSnapshot capture(
            ServerPlayer player
    ) {

        GhostRebootSnapshot snapshot =
                new GhostRebootSnapshot();

        snapshot.x =
                player.getX();

        snapshot.y =
                player.getY();

        snapshot.z =
                player.getZ();

        snapshot.yaw =
                player.getYRot();

        snapshot.pitch =
                player.getXRot();

        snapshot.health =
                player.getHealth();

        snapshot.food =
                player.getFoodData().getFoodLevel();

        Map<ResourceLocation, PossessedGhostState> states =
                PossessionHandler.getAllStates(player);

        snapshot.ghosts.putAll(states);

        return snapshot;
    }
}