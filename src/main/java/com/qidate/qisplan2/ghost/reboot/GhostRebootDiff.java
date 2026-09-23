package com.qidate.qisplan2.ghost.reboot;

import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GhostRebootDiff {

    public Double dx;
    public Double dy;
    public Double dz;

    public Float dyaw;
    public Float dpitch;

    public Float health;
    public Float saturation;
    public Integer food;

    public Integer previousExperienceLevel;
    public Float previousExperienceProgress;
    public Integer previousTotalExperience;

    public List<MobEffectInstance> previousEffects;

    public final Map<Integer, ItemStack> inventoryChanges =
            new HashMap<>();

    public final Map<ResourceLocation, PossessedGhostState> ghostChanges =
            new HashMap<>();

    public boolean isEmpty() {

        return dx == null
                && dy == null
                && dz == null
                && dyaw == null
                && dpitch == null
                && health == null
                && food == null
                && saturation == null
                && previousExperienceLevel == null
                && previousExperienceProgress == null
                && previousTotalExperience == null
                && previousEffects == null
                && inventoryChanges.isEmpty()
                && ghostChanges.isEmpty();
    }
}