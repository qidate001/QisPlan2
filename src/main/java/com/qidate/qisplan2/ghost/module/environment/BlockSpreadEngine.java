package com.qidate.qisplan2.ghost.module.environment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 通用蔓延算法，只依赖方块坐标和世界操作接口，不识别鬼地毯或模块宿主。
 * 先找种子、收集有界连通群落，再按链式或边缘扩张策略放置。
 */
public final class BlockSpreadEngine {
    private static final List<Direction> HORIZONTAL =
            List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    private static final List<Direction> VOLUME =
            List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN);

    /** 工具类不需要实例化。 */
    private BlockSpreadEngine() {}

    /** 执行一次蔓延并返回实际新增数量；没有种子或合法位置时返回零。 */
    public static int spread(BlockSpreadAccess world, BlockPos origin, BlockSpreadSpec spec) {
        if (spec.maxCount() == 0) return 0;
        List<Direction> directions = spec.shape() == BlockSpreadSpec.Shape.HORIZONTAL_2D ? HORIZONTAL : VOLUME;
        BlockPos seed = findSeed(world, origin, spec);
        if (seed == null) return 0;
        Set<BlockPos> group = collect(world, seed, directions, spec.maxVisited());
        int count = spec.minCount() + world.nextInt(spec.maxCount() - spec.minCount() + 1);
        if (count == 0) return 0;
        BlockPos current = seed;
        if (spec.pattern() == BlockSpreadSpec.Pattern.CHAIN) {
            var edges = new ArrayList<BlockPos>();
            for (BlockPos pos : group)
                if (!candidates(world, Set.of(pos), group, directions).isEmpty()) edges.add(pos);
            if (edges.isEmpty()) return 0;
            current = edges.get(world.nextInt(edges.size()));
        }
        int placed = 0;
        while (placed < count) {
            Set<BlockPos> from = spec.pattern() == BlockSpreadSpec.Pattern.CHAIN ? Set.of(current) : group;
            var options = candidates(world, from, group, directions);
            boolean succeeded = false;
            // 放置被世界拒绝时继续尝试本轮其他候选，不把失败算进数量。
            while (!options.isEmpty()) {
                BlockPos next = options.remove(world.nextInt(options.size()));
                if (!world.available(next) || world.isSeed(next) || !world.canPlace(next) || !world.place(next))
                    continue;
                group.add(next);
                current = next;
                placed++;
                succeeded = true;
                break;
            }
            if (!succeeded) break;
        }
        return placed;
    }

    /** 按切比雪夫距离由近到远找种子；二维模式严格保持起点高度。 */
    private static BlockPos findSeed(BlockSpreadAccess world, BlockPos origin, BlockSpreadSpec spec) {
        for (int radius = 0; radius <= spec.searchRadius(); radius++) {
            int vertical = spec.shape() == BlockSpreadSpec.Shape.VOLUME_3D ? radius : 0;
            for (int x = -radius; x <= radius; x++)
                for (int y = -vertical; y <= vertical; y++)
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z)) != radius) continue;
                        BlockPos pos = origin.offset(x, y, z);
                        if (world.available(pos) && world.isSeed(pos)) return pos.immutable();
                    }
        }
        return null;
    }

    /** 广度优先收集相连群落，达到预算即停止，不访问未加载位置。 */
    private static Set<BlockPos> collect(BlockSpreadAccess world, BlockPos seed,
                                         List<Direction> directions, int limit) {
        Set<BlockPos> group = new LinkedHashSet<>();
        var queue = new ArrayDeque<BlockPos>();
        group.add(seed);
        queue.add(seed);
        while (!queue.isEmpty() && group.size() < limit) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : directions) {
                BlockPos next = current.relative(direction);
                if (group.contains(next) || !world.available(next) || !world.isSeed(next)) continue;
                group.add(next);
                queue.addLast(next);
                if (group.size() >= limit) break;
            }
        }
        return group;
    }

    /** 返回可新增位置的去重列表，避免同一空格因邻接多个群落方块而被重复计权。 */
    private static List<BlockPos> candidates(BlockSpreadAccess world, Set<BlockPos> from,
                                             Set<BlockPos> group, List<Direction> directions) {
        Set<BlockPos> result = new LinkedHashSet<>();
        for (BlockPos pos : from)
            for (Direction direction : directions) {
                BlockPos next = pos.relative(direction);
                if (!group.contains(next) && world.available(next) && !world.isSeed(next) && world.canPlace(next))
                    result.add(next);
            }
        return new ArrayList<>(result);
    }
}
