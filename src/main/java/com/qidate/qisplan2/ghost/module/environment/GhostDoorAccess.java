package com.qidate.qisplan2.ghost.module.environment;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;

/** 门的空间与传送出口。只提供门、穿门者及路由事实，不暴露能力来源的载体。 */
public interface GhostDoorAccess {
    GhostDoorAccess NONE = new GhostDoorAccess() {};
    /** 已验证的目的门；位置统一使用门下半部。 */
    record Destination(ResourceKey<Level> dimension, BlockPos pos, boolean open) {}
    /** 查询正面近距离可作用的门；固定门位置由适配器提供相同的媒介事实。 */
    default BlockPos frontDoor() { return null; }
    /** 查询当前关联的门：静态锚点或正在穿越的门。 */
    default BlockPos linkedDoor() { return null; }
    /** 查询门是否仍然存在且处于打开状态。 */
    default boolean open(BlockPos pos) { return false; }
    /** 关闭门的上下半部，并播放世界表现。 */
    default boolean close(BlockPos pos) { return false; }
    /** 返回门前、门后及门洞内的存活目标。 */
    default List<LivingEntity> nearby(BlockPos pos) { return List.of(); }
    /** 确认目标处于门洞内，且当前没有穿门冷却。 */
    default boolean inside(LivingEntity target, BlockPos pos) { return false; }
    /** 维护当前来源的实时门端点；无有效门或失去暴露时撤销。 */
    default void maintainLink(int number) {}
    /** 按门牌号解析去重后的其他目的门；不任意挑选一对多中的一个目标。 */
    default List<Destination> destinations(int number, BlockPos origin) { return List.of(); }
    /** 向目的门传送；voidTravel 表示关闭目的门导致的虚空出口。 */
    default boolean travel(LivingEntity target, Destination destination, boolean voidTravel) { return false; }
    /** 消耗本次穿门尝试，避免失败袭击在每个 tick 无限重复。 */
    default void cooldown(LivingEntity target) {}
}
