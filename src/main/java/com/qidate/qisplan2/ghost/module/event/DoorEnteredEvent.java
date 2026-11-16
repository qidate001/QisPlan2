package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/** 实体处于某扇门的门洞内这一空间事实；不是传送请求。 */
public record DoorEnteredEvent(LivingEntity target, BlockPos door) implements GhostEvent {
    /** 保存不可变门坐标，防止扫描循环继续修改它。 */
    public DoorEnteredEvent { door = door.immutable(); }
}
