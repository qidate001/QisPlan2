package com.qidate.qisplan2.ghost.module.environment;

import net.minecraft.core.BlockPos;

/** 蔓延算法使用的最小世界接口；方块状态、放置检查及随机源均由实现提供。 */
public interface BlockSpreadAccess {
    /** 当前位置是否在有效且已加载的范围内；不得为了蔓延主动加载区块。 */
    boolean available(BlockPos pos);

    /** 判断当前位置是否已属于本次目标方块的群落。 */
    boolean isSeed(BlockPos pos);

    /** 检查替换、支撑和方块生存条件，不产生世界修改。 */
    boolean canPlace(BlockPos pos);

    /** 再次校验后尝试放置；只有成功保留下来的新方块才返回 true。 */
    boolean place(BlockPos pos);

    /** 返回 [0, bound) 内的随机整数；bound 必须为正数。 */
    int nextInt(int bound);
}
