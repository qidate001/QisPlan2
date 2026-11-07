package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;

/**
 * 某个生物听见了指定门处的敲门声。
 *
 * 公共门系统在声音产生并确认听者时立即派发此事实。
 * source 是敲门者，door 是此次发声位置（包括鬼门回声），target 是听者。
 * 延迟、袭击强度和裁决结果属于规则执行流程，不属于听声事件。
 */
public record DoorKnockHeardEvent(LivingEntity source, BlockPos door,
        LivingEntity target) implements GhostEvent {
    public DoorKnockHeardEvent {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        door = Objects.requireNonNull(door, "door").immutable();
    }
}
