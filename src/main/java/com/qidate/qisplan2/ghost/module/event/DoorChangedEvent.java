package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Objects;

/**
 * 服务端确认的门状态变化事件。
 *
 * doorPos 始终指向双格门的下半方块位置；opening 为 true 表示开门，
 * 为 false 表示关门。各厉鬼模块可以基于同一事件选择自己的触发规律。
 */
public record DoorChangedEvent(
        LivingEntity source,
        BlockPos doorPos,
        BlockState doorState,
        boolean opening
) implements GhostEvent {
    public DoorChangedEvent {
        Objects.requireNonNull(source, "source");
        doorPos = Objects.requireNonNull(doorPos, "doorPos").immutable();
        Objects.requireNonNull(doorState, "doorState");
    }
}
