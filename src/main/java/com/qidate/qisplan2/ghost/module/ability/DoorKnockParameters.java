package com.qidate.qisplan2.ghost.module.ability;

import net.minecraft.core.BlockPos;
import java.util.Objects;

/** 敲门能力的目标门参数；它是调用输入，不参与语义事件分发。 */
public record DoorKnockParameters(BlockPos door) implements GhostAbilityParameters {
    public DoorKnockParameters {
        door = Objects.requireNonNull(door, "door").immutable();
    }
}
