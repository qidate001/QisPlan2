package com.qidate.qisplan2.ghost.module.environment;

import net.minecraft.world.entity.LivingEntity;

/** 当前模块的鬼域操作接口，仅暴露世界事实与基础操作，不定义技能 ID 或技能列表。 */
public interface GhostDomainAccess {
    GhostDomainAccess NONE = new GhostDomainAccess() {};
    /** 设置开关；明确目标状态，不依赖客户端猜测当前是否展开。 */
    default boolean setOpen(boolean open) { return false; }
    /** 为有效覆盖中的目标设置绝对层数；非法层数或目标返回失败。 */
    default boolean setTargetLayer(LivingEntity target, int layer) { return false; }
    /** 查询当前鬼域是否覆盖指定世界目标。 */
    default boolean contains(LivingEntity target) { return false; }
    /** 当前鬼域的灵异强度。 */
    default double intensity() { return 0; }
    /** 当前模块是否存在有效鬼域。 */
    default boolean exists() { return false; }
    /** 获取鬼域层数，缺少鬼域时为零。 */
    default int layer() { return 0; }
    /** 设置鬼域总层数；框架统一限制在 1～10，技能只决定调整方式。 */
    default boolean setLayer(int value) { return false; }
    /** 读取可移动作用对象所在层数。 */
    default int occupiedLayer() { return 0; }
    /** 设置可移动作用对象所在层数。 */
    default boolean setOccupiedLayer(int value) { return false; }
    /** 在当前鬼域内传送到输入指向位置。 */
    default boolean teleportToAim() { return false; }
    /** 在当前鬼域内调整指向目标所在层数。 */
    default boolean shiftTargetLayer(int delta) { return false; }
    /** 切换当前来源的持续重启状态。 */
    default boolean toggleReboot() { return false; }
}
