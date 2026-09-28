package com.qidate.qisplan2.ghost.curse.type.ghosttombstone;

import com.qidate.qisplan2.ghost.curse.CurseType;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 鬼墓碑诅咒类型。
 */
public class GhostTombstoneCurseType
        implements CurseType {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    "qisplan2",
                    "ghost_tombstone"
            );

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /**
     * 创建鬼墓碑诅咒实例。
     */
    public GhostTombstoneCurse create(
            UUID target,
            GhostTombstoneCurseSource source
    ) {
        return new GhostTombstoneCurse(
                target,
                source
        );
    }
}