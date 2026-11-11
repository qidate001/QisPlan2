package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.ghost.module.environment.BlockSpreadAccess;
import com.qidate.qisplan2.ghost.module.environment.BlockSpreadEngine;
import com.qidate.qisplan2.ghost.module.environment.BlockSpreadSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;

/** 将通用蔓延查询与放置落实到服务端世界，不决定模块规则或辨别来源载体。 */
public final class BlockSpreadWorldAdapter {
    /** 工具类不创建实例。 */
    private BlockSpreadWorldAdapter() {}

    /**
     * 根据注册标识解析要蔓延的方块，使用其默认状态放置。
     * 未注册方块或空气不产生效果；返回实际成功放置的数量。
     */
    public static int spread(ServerLevel level, BlockPos origin, BlockSpreadSpec spec) {
        if (!BuiltInRegistries.BLOCK.containsKey(spec.block())) return 0;
        var block = BuiltInRegistries.BLOCK.get(spec.block());
        var state = block.defaultBlockState();
        if (state.isAir()) return 0;
        return BlockSpreadEngine.spread(new BlockSpreadAccess() {
            /** 不超出建造高度和世界边界，也不主动加载区块。 */
            public boolean available(BlockPos pos) {
                return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                        && level.hasChunkAt(pos);
            }

            /** 以方块身份识别群落，允许同种方块拥有不同的状态属性。 */
            public boolean isSeed(BlockPos pos) { return level.getBlockState(pos).is(block); }

            /** 分别检查替换策略、可选的下方支撑，以及目标方块自身的生存条件。 */
            public boolean canPlace(BlockPos pos) {
                if (!available(pos)) return false;
                var existing = level.getBlockState(pos);
                if (existing.is(block)) return false;
                boolean replaceable = spec.replacement() == BlockSpreadSpec.Replacement.AIR_ONLY
                        ? existing.isAir() : existing.canBeReplaced();
                if (!replaceable) return false;
                if (spec.requireSupport() && (!available(pos.below())
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)))
                    return false;
                // 六邻域已加载，避免 canSurvive 的常见邻居查询隐式加载边缘区块。
                for (Direction direction : Direction.values())
                    if (!available(pos.relative(direction))) return false;
                return state.canSurvive(level, pos);
            }

            /** 更新邻居后再次确认方块仍存在，失败或立即脱落不计入新增数量。 */
            public boolean place(BlockPos pos) {
                return canPlace(pos) && level.setBlock(pos, state, 3) && level.getBlockState(pos).is(block);
            }

            /** 使用世界随机源，保持服务端统一决定蔓延结果。 */
            public int nextInt(int bound) { return level.random.nextInt(bound); }
        }, origin, spec);
    }
}
