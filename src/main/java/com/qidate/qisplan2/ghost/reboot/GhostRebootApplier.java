package com.qidate.qisplan2.ghost.reboot;

import com.qidate.qisplan2.core.ModGameRules;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;

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

        // 位置 & 视角
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

        // 生命值
        player.setHealth(
                snapshot.health
        );

        // 饥饿值
        player.getFoodData().setFoodLevel(
                snapshot.food
        );

        // 饱和值
        player.getFoodData().setSaturation(
                snapshot.saturation
        );

        // 经验值
        player.experienceLevel =
                snapshot.experienceLevel;

        player.experienceProgress =
                snapshot.experienceProgress;

        player.totalExperience =
                snapshot.totalExperience;

        player.connection.send(
                new ClientboundSetExperiencePacket(
                        player.experienceProgress,
                        player.experienceLevel,
                        player.totalExperience
                )
        );

        // 药水效果
        player.removeAllEffects();

        for (MobEffectInstance effect :
                snapshot.effects) {

            player.addEffect(
                    new MobEffectInstance(effect)
            );
        }

        // 物品栏
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