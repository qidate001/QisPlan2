package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import com.qidate.qisplan2.core.ModGameRules;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class GhostRebootDiffApplier {

    private GhostRebootDiffApplier() {
    }

    public static void applyReverse(
            ServerPlayer player,
            GhostRebootDiff diff
    ) {

        // 位置与视角
        boolean restorePosition =
                player.level()
                        .getGameRules()
                        .getBoolean(
                                ModGameRules.GHOST_REBOOT_RESTORE_POSITION
                        );

        if (restorePosition) {

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
        }

        // 生命值
        if (diff.health != null) {

            player.setHealth(
                    player.getHealth()
                            - diff.health
            );
        }

        // 饥饿值
        if (diff.food != null) {

            player.getFoodData().setFoodLevel(
                    player.getFoodData().getFoodLevel()
                            - diff.food
            );
        }

        if (diff.saturation != null) {

            player.getFoodData().setSaturation(
                    player.getFoodData().getSaturationLevel()
                            - diff.saturation
            );
        }

        // 经验值
        if (diff.previousExperienceLevel != null) {

            player.experienceLevel =
                    diff.previousExperienceLevel;

            player.experienceProgress =
                    diff.previousExperienceProgress;

            player.totalExperience =
                    diff.previousTotalExperience;

            player.onUpdateAbilities();

            player.connection.send(
                    new ClientboundSetExperiencePacket(
                            player.experienceProgress,
                            player.experienceLevel,
                            player.totalExperience
                    )
            );
        }

        // 药水效果
        if (diff.previousEffects != null) {

            player.removeAllEffects();

            for (MobEffectInstance effect :
                    diff.previousEffects) {

                player.addEffect(
                        new MobEffectInstance(effect)
                );
            }
        }

        // 背包
        boolean restoreInventory =
                player.level()
                        .getGameRules()
                        .getBoolean(
                                ModGameRules.GHOST_REBOOT_RESTORE_INVENTORY
                        );

        if (restoreInventory
                && !diff.inventoryChanges.isEmpty()) {

            Inventory inventory =
                    player.getInventory();

            for (Map.Entry<Integer, ItemStack> entry
                    : diff.inventoryChanges.entrySet()) {

                setInventorySlot(
                        inventory,
                        entry.getKey(),
                        entry.getValue()
                );
            }

            inventory.setChanged();
        }
    }

    private static void setInventorySlot(
            Inventory inventory,
            int slot,
            ItemStack stack
    ) {

        if (slot < inventory.items.size()) {

            inventory.items.set(
                    slot,
                    stack.copy()
            );

            return;
        }

        int armorIndex =
                slot - inventory.items.size();

        if (armorIndex < inventory.armor.size()) {

            inventory.armor.set(
                    armorIndex,
                    stack.copy()
            );

            return;
        }

        int offhandIndex =
                armorIndex - inventory.armor.size();

        if (offhandIndex < inventory.offhand.size()) {

            inventory.offhand.set(
                    offhandIndex,
                    stack.copy()
            );
        }
    }
}