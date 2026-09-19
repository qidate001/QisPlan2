package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import com.qidate.qisplan2.ghost.PossessedGhostState;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class GhostRebootDiffBuilder {

    private GhostRebootDiffBuilder() {
    }

    public static GhostRebootDiff build(
            GhostRebootSnapshot previous,
            GhostRebootSnapshot current
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
}