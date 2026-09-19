package com.qidate.qisplan2.ghost.ability.ghosteye.reboot;

import com.qidate.qisplan2.ghost.PossessedGhostState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GhostRebootSnapshot {

    public double x;
    public double y;
    public double z;

    public float yaw;
    public float pitch;

    public float health;
    public int food;
    public float saturation;

    public int experienceLevel;
    public float experienceProgress;
    public int totalExperience;

    public final List<ItemStack> inventory =
            new ArrayList<>();

    public final List<MobEffectInstance> effects =
            new ArrayList<>();

    public final Map<ResourceLocation, PossessedGhostState> ghosts =
            new HashMap<>();
}