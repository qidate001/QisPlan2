package com.qidate.qisplan2.ghost.isolation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public final class GhostIsolationCuboid {

    private final BlockPos min;
    private final BlockPos max;

    public GhostIsolationCuboid(
            BlockPos min,
            BlockPos max
    ) {
        this.min = min.immutable();
        this.max = max.immutable();
    }

    public BlockPos getMin() {
        return min;
    }

    public BlockPos getMax() {
        return max;
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX()
                && pos.getX() <= max.getX()
                && pos.getY() >= min.getY()
                && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ()
                && pos.getZ() <= max.getZ();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.put(
                "Min",
                BlockPos.CODEC
                        .encodeStart(
                                net.minecraft.nbt.NbtOps.INSTANCE,
                                min
                        )
                        .getOrThrow()
        );

        tag.put(
                "Max",
                BlockPos.CODEC
                        .encodeStart(
                                net.minecraft.nbt.NbtOps.INSTANCE,
                                max
                        )
                        .getOrThrow()
        );

        return tag;
    }

    public static GhostIsolationCuboid load(
            CompoundTag tag
    ) {
        try {
            BlockPos min =
                    BlockPos.CODEC
                            .parse(
                                    net.minecraft.nbt.NbtOps.INSTANCE,
                                    tag.get("Min")
                            )
                            .getOrThrow();

            BlockPos max =
                    BlockPos.CODEC
                            .parse(
                                    net.minecraft.nbt.NbtOps.INSTANCE,
                                    tag.get("Max")
                            )
                            .getOrThrow();

            return new GhostIsolationCuboid(
                    min,
                    max
            );

        } catch (Exception exception) {
            return null;
        }
    }
}