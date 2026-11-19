package com.qidate.qisplan2.ghost.module.environment;

import net.minecraft.world.entity.LivingEntity;

/** 插入/附着能力端口；模块只看媒介和结果，不接触附着容器或载体身份。 */
public interface GhostAttachmentAccess {
    GhostAttachmentAccess NONE = new GhostAttachmentAccess() {};
    /** 目标是否具备可附着的表面、灵异模块以及当前可达距离。 */
    default boolean canEmbed(LivingEntity target) { return false; }
    /** 将来源作为独立附着物放到目标表面，保留原模块数据而不交给目标驾驭。 */
    default boolean embed(LivingEntity target) { return false; }
    /** 是否仍然处于插入状态；只由附着适配器解释。 */
    default boolean embedded() { return false; }
}
