package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import net.minecraft.server.level.ServerPlayer;

public final class GhostRebootApplier {

    private GhostRebootApplier() {
    }

    public static void apply(
            ServerPlayer player,
            GhostRebootSnapshot snapshot
    ) {
        player.teleportTo(
                snapshot.x,
                snapshot.y,
                snapshot.z
        );

        player.setYRot(snapshot.yaw);
        player.setXRot(snapshot.pitch);

        player.setHealth(snapshot.health);

        player.getFoodData().setFoodLevel(snapshot.food);
    }
}