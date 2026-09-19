package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import com.qidate.qisplan2.ghost.PossessedGhostState;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class GhostRebootDiff {

    public Double dx;
    public Double dy;
    public Double dz;

    public Float health;

    public Integer food;

    public final Map<ResourceLocation, PossessedGhostState> ghostChanges =
            new HashMap<>();

    public boolean isEmpty() {

        return dx == null
                && dy == null
                && dz == null
                && health == null
                && food == null
                && ghostChanges.isEmpty();
    }
}