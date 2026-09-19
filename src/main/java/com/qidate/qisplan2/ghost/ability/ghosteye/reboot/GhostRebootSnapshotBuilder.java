package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

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

        Inventory inventory =
                player.getInventory();

        for (ItemStack stack : inventory.items) {

            snapshot.inventory.add(
                    stack.copy()
            );
        }

        for (ItemStack stack : inventory.armor) {

            snapshot.inventory.add(
                    stack.copy()
            );
        }

        for (ItemStack stack : inventory.offhand) {

            snapshot.inventory.add(
                    stack.copy()
            );
        }

        Map<ResourceLocation, PossessedGhostState> states =
                PossessionHandler.getAllStates(player);

        snapshot.ghosts.putAll(states);

        return snapshot;
    }
}