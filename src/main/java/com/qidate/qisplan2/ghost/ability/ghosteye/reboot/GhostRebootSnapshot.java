package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import com.qidate.qisplan2.ghost.PossessedGhostState;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class GhostRebootSnapshot {

    public double x;
    public double y;
    public double z;

    public float yaw;
    public float pitch;

    public float health;
    public int food;

    public final Map<ResourceLocation, PossessedGhostState> ghosts =
            new HashMap<>();
}