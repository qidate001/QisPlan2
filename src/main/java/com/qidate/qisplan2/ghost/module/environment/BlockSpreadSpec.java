package com.qidate.qisplan2.ghost.module.environment;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * 不依赖载体的方块蔓延配置。
 * 数量指成功新增的方块数，不包含已有群落；空间不足时允许少于请求数量。
 * 搜索半径用于寻找已有种子，不是限制群落尺寸；maxVisited 限制本轮群落遍历规模。
 * 配置只描述世界操作，不能包含或查询模块宿主。
 */
public record BlockSpreadSpec(ResourceLocation block, int minCount, int maxCount,
                              Shape shape, Pattern pattern, Replacement replacement,
                              boolean requireSupport, int searchRadius, int maxVisited) {
    /** 群落连接、种子搜索和新增方块统一使用此方向范围。 */
    public enum Shape {
        /** 同一高度的水平四邻域。 */
        HORIZONTAL_2D,
        /** 上下左右前后六邻域。 */
        VOLUME_3D
    }

    /** 选择下一次生长位置的方式。 */
    public enum Pattern {
        /** 从随机群落边缘开始，后续沿最新生成的方块继续；走入死路即结束。 */
        CHAIN,
        /** 每次从当前群落可放置的边缘位置中随机选择，可以形成分支。 */
        FRONTIER
    }

    /** 空间替换策略；两种策略都不覆盖已有的同类群落方块。 */
    public enum Replacement {
        /** 只允许填充空气。 */
        AIR_ONLY,
        /** 允许替换世界状态明确标记为可替换的方块。 */
        REPLACEABLE
    }

    /** 校验配置并约束单次调用成本，拒绝负数量、倒置区间与无界搜索。 */
    public BlockSpreadSpec {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(replacement, "replacement");
        if (minCount < 0 || maxCount < minCount || maxCount > 4096)
            throw new IllegalArgumentException("Spread count must satisfy 0 <= min <= max <= 4096");
        if (searchRadius < 0 || searchRadius > 16)
            throw new IllegalArgumentException("Seed search radius must be within 0..16");
        if (maxVisited < 1 || maxVisited > 65536)
            throw new IllegalArgumentException("Group traversal limit must be within 1..65536");
    }
}
