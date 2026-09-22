package com.qidate.qisplan2.ghost.reboot;

import com.qidate.qisplan2.ghost.PossessedGhostState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GhostRebootDiffBuilder {

    private GhostRebootDiffBuilder() {
    }

    public static GhostRebootDiff build(
            GhostRebootSnapshot previous,
            GhostRebootSnapshot current,
            boolean restoreInventory
    ) {

        GhostRebootDiff diff =
                new GhostRebootDiff();

        // 位置变化
        if (previous.x != current.x) {
            diff.dx =
                    current.x - previous.x;
        }

        if (previous.y != current.y) {
            diff.dy =
                    current.y - previous.y;
        }

        if (previous.z != current.z) {
            diff.dz =
                    current.z - previous.z;
        }

        // 视野变化
        if (previous.yaw != current.yaw) {

            diff.dyaw =
                    current.yaw - previous.yaw;
        }

        if (previous.pitch != current.pitch) {

            diff.dpitch =
                    current.pitch - previous.pitch;
        }

        // 生命值变化
        if (previous.health != current.health) {

            diff.health =
                    current.health - previous.health;
        }

        // 饥饿值变化
        if (previous.food != current.food) {

            diff.food =
                    current.food - previous.food;
        }

        if (previous.saturation != current.saturation) {

            diff.saturation =
                    current.saturation - previous.saturation;
        }

        // 经验值
        if (previous.experienceLevel != current.experienceLevel
                || previous.experienceProgress != current.experienceProgress
                || previous.totalExperience != current.totalExperience) {

            diff.previousExperienceLevel =
                    previous.experienceLevel;

            diff.previousExperienceProgress =
                    previous.experienceProgress;

            diff.previousTotalExperience =
                    previous.totalExperience;
        }

        // 药水效果
        if (!effectsEqual(
                previous.effects,
                current.effects
        )) {

            diff.previousEffects =
                    new ArrayList<>();

            for (MobEffectInstance effect :
                    previous.effects) {

                diff.previousEffects.add(
                        new MobEffectInstance(effect)
                );
            }
        }

        // 物品栏变化
        if (restoreInventory) {

            int inventorySize =
                    Math.min(
                            previous.inventory.size(),
                            current.inventory.size()
                    );

            for (int i = 0; i < inventorySize; i++) {

                ItemStack previousStack =
                        previous.inventory.get(i);

                ItemStack currentStack =
                        current.inventory.get(i);

                if (!ItemStack.matches(
                        previousStack,
                        currentStack
                )) {

                    diff.inventoryChanges.put(
                            i,
                            previousStack.copy()
                    );
                }
            }
        }

        // 驭鬼状态变化
        Set<ResourceLocation> ids =
                new HashSet<>();

        ids.addAll(previous.ghosts.keySet());
        ids.addAll(current.ghosts.keySet());

        for (ResourceLocation id : ids) {

            PossessedGhostState oldState =
                    previous.ghosts.get(id);

            PossessedGhostState newState =
                    current.ghosts.get(id);

            if (oldState == null || newState == null) {

                diff.ghostChanges.put(
                        id,
                        newState
                );

                continue;
            }

            if (!oldState.equals(newState)) {

                diff.ghostChanges.put(
                        id,
                        newState
                );
            }
        }

        return diff;
    }

    private static boolean effectsEqual(
            List<MobEffectInstance> previous,
            List<MobEffectInstance> current
    ) {

        if (previous.size() != current.size()) {
            return false;
        }

        for (MobEffectInstance previousEffect : previous) {

            boolean found = false;

            for (MobEffectInstance currentEffect : current) {

                if (previousEffect.equals(currentEffect)) {

                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }
}