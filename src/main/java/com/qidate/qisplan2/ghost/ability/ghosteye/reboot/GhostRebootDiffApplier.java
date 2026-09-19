package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import net.minecraft.server.level.ServerPlayer;

public final class GhostRebootDiffApplier {

    private GhostRebootDiffApplier() {
    }

    public static void applyReverse(
            ServerPlayer player,
            GhostRebootDiff diff
    ) {

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (diff.dx != null) {
            x -= diff.dx;
        }

        if (diff.dy != null) {
            y -= diff.dy;
        }

        if (diff.dz != null) {
            z -= diff.dz;
        }

        if (diff.dx != null
                || diff.dy != null
                || diff.dz != null) {

            player.teleportTo(
                    x,
                    y,
                    z
            );
        }

        if (diff.health != null) {

            player.setHealth(
                    player.getHealth()
                            - diff.health
            );
        }

        if (diff.food != null) {

            player.getFoodData().setFoodLevel(
                    player.getFoodData().getFoodLevel()
                            - diff.food
            );
        }
    }
}