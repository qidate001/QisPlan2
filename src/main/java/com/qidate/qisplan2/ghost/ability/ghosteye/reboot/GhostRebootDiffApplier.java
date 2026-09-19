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

        float yaw =
                player.getYRot();

        float pitch =
                player.getXRot();

        if (diff.dyaw != null) {
            yaw -= diff.dyaw;
        }

        if (diff.dpitch != null) {
            pitch -= diff.dpitch;
        }

        if (diff.dx != null
                || diff.dy != null
                || diff.dz != null
                || diff.dyaw != null
                || diff.dpitch != null) {

            player.teleportTo(
                    player.serverLevel(),
                    x,
                    y,
                    z,
                    yaw,
                    pitch
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