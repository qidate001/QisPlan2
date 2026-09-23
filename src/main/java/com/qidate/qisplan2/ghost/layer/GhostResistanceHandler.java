package com.qidate.qisplan2.ghost.layer;

import com.qidate.qisplan2.core.ModAttachments;
import net.minecraft.world.entity.Entity;

public final class GhostResistanceHandler {

    private GhostResistanceHandler() {
    }

    public static int getResistance(
            Entity entity
    ) {

        return Math.max(
                0,
                entity.getData(
                        ModAttachments.GHOST_RESISTANCE
                )
        );
    }

    public static void setResistance(
            Entity entity,
            int resistance
    ) {

        entity.setData(
                ModAttachments.GHOST_RESISTANCE,
                Math.max(0, resistance)
        );
    }

    public static void addResistance(
            Entity entity,
            int delta
    ) {

        setResistance(
                entity,
                getResistance(entity) + delta
        );
    }
}