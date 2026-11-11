package com.qidate.qisplan2.ghost.module.environment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/** 当前模块与某个目标之间的专属状态；存储位置由载体适配层决定。 */
public interface GhostTargetState {
    /** 读取目标关联状态的副本；无记录时返回空标签。 */
    CompoundTag read(LivingEntity target);

    /** 保存目标关联状态；模块不接触载体，也不自行选择实体附件或物品组件。 */
    void write(LivingEntity target, CompoundTag data);
}
