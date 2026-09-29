package com.qidate.qisplan2.ghost.curse.type.ghostdivination;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseSource;
import com.qidate.qisplan2.ghost.curse.CurseType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class GhostDivinationCurseType
        implements CurseType {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "ghost_divination"
            );

    public static final ResourceLocation ICON =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "textures/item/ghost_divination_slip.png"
            );

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Curse create(
            UUID target,
            CurseSource source,
            CompoundTag initialState
    ) {

        int strength =
                initialState.contains("Strength")
                        ? initialState.getInt("Strength")
                        : GhostDivinationCurse.DEFAULT_STRENGTH;

        int remainingTicks =
                initialState.contains("RemainingTicks")
                        ? initialState.getInt("RemainingTicks")
                        : GhostDivinationCurse.DEFAULT_DURATION;

        return new GhostDivinationCurse(
                target,
                (GhostDivinationCurseSource) source,
                strength,
                remainingTicks
        );
    }

    @Override
    public ResourceLocation icon() {
        return ICON;
    }

    @Override
    public boolean canDetect() {
        return true;
    }

    @Override
    public Curse load(
            CompoundTag tag
    ) {

        UUID id =
                tag.getUUID("Id");

        UUID target =
                tag.getUUID("Target");

        GhostDivinationCurseSource source =
                GhostDivinationCurseSource.load(
                        tag.getCompound("Source")
                );

        int strength =
                tag.getInt("Strength");

        int remainingTicks =
                tag.getInt("RemainingTicks");

        return new GhostDivinationCurse(
                id,
                target,
                source,
                strength,
                remainingTicks
        );
    }
}