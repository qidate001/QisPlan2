package com.qidate.qisplan2.ghost.domain;

import net.minecraft.core.BlockPos;

public interface GhostDomainShape {

    boolean contains(
            double centerX,
            double centerY,
            double centerZ,
            double x,
            double y,
            double z
    );

    default boolean contains(
            double centerX,
            double centerY,
            double centerZ,
            BlockPos pos
    ) {

        return contains(
                centerX,
                centerY,
                centerZ,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );
    }
}