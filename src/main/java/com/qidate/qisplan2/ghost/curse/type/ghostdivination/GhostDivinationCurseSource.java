package com.qidate.qisplan2.ghost.curse.type.ghostdivination;

import com.qidate.qisplan2.ghost.curse.CurseSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class GhostDivinationCurseSource
        implements CurseSource {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "ghost_divination"
            );

    @Override
    public ResourceLocation getType() {
        return ID;
    }

    @Override
    public CompoundTag save() {
        return new CompoundTag();
    }

    public static GhostDivinationCurseSource load(
            CompoundTag tag
    ) {
        return new GhostDivinationCurseSource();
    }
}