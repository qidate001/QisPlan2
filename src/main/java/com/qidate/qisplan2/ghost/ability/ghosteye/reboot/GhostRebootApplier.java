package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import com.qidate.qisplan2.core.ModGameRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class GhostRebootApplier {

    private GhostRebootApplier() {
    }

    public static void apply(
            ServerPlayer player,
            GhostRebootSnapshot snapshot
    ) {

        boolean restorePosition =
                player.level()
                        .getGameRules()
                        .getBoolean(
                                ModGameRules.GHOST_REBOOT_RESTORE_POSITION
                        );

        boolean restoreInventory =
                player.level()
                        .getGameRules()
                        .getBoolean(
                                ModGameRules.GHOST_REBOOT_RESTORE_INVENTORY
                        );

        if (restorePosition) {

            player.teleportTo(
                    player.serverLevel(),
                    snapshot.x,
                    snapshot.y,
                    snapshot.z,
                    snapshot.yaw,
                    snapshot.pitch
            );
        }

        player.setHealth(
                snapshot.health
        );

        player.getFoodData().setFoodLevel(
                snapshot.food
        );

        if (restoreInventory) {

            restoreInventory(
                    player,
                    snapshot
            );
        }
    }

    private static void restoreInventory(
            ServerPlayer player,
            GhostRebootSnapshot snapshot
    ) {

        Inventory inventory =
                player.getInventory();

        for (int i = 0; i < inventory.items.size(); i++) {

            inventory.items.set(
                    i,
                    snapshot.inventory
                            .get(i)
                            .copy()
            );
        }

        int offset =
                inventory.items.size();

        for (int i = 0; i < inventory.armor.size(); i++) {

            inventory.armor.set(
                    i,
                    snapshot.inventory
                            .get(offset + i)
                            .copy()
            );
        }

        offset += inventory.armor.size();

        for (int i = 0; i < inventory.offhand.size(); i++) {

            inventory.offhand.set(
                    i,
                    snapshot.inventory
                            .get(offset + i)
                            .copy()
            );
        }

        inventory.setChanged();
    }
}