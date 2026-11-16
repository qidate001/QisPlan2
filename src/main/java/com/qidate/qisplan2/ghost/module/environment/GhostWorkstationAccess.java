package com.qidate.qisplan2.ghost.module.environment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** 通用工作台出口。模块声明工作台类型，适配器负责菜单、配方、库存与持久化。 */
public interface GhostWorkstationAccess {
    GhostWorkstationAccess NONE = new GhostWorkstationAccess() {};
    /** 查询指定工作台能否被交互者使用，不暴露来源载体。 */
    default boolean canOpen(ResourceLocation type, LivingEntity user) { return false; }
    /** 打开指定工作台，服务端仍须再次验证交互距离与模块归属。 */
    default boolean open(ResourceLocation type, LivingEntity user) { return false; }
    /** 维护已经存在的工作台库存与制作进度，不启动自动袭击。 */
    default void tick(ResourceLocation type) {}
}
