package com.qidate.qisplan2.ghost.partition;

import net.minecraft.core.Direction;

public record PartitionRoomPos(
        int x,
        int y,
        int z
) {

    public PartitionRoomPos relative(
            Direction direction
    ) {

        return new PartitionRoomPos(
                x + direction.getStepX(),
                y + direction.getStepY(),
                z + direction.getStepZ()
        );
    }
}