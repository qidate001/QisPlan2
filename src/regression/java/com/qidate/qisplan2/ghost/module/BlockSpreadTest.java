package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.environment.BlockSpreadAccess;
import com.qidate.qisplan2.ghost.module.environment.BlockSpreadEngine;
import com.qidate.qisplan2.ghost.module.environment.BlockSpreadSpec;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** 用独立网格实现世界接口，验证蔓延算法不依赖实体、方块注册表或鬼地毯。 */
public final class BlockSpreadTest {
    private static final BlockPos ORIGIN = BlockPos.ZERO;
    private static final ResourceLocation BLOCK = ResourceLocation.fromNamespaceAndPath("test", "spread");

    /** 回归检查由统一测试入口调用。 */
    private BlockSpreadTest() {}

    /** 测试网格区分已有群落、可放置位置、未加载位置与放置失败，避免把查询等同于成功。 */
    private static final class Grid implements BlockSpreadAccess {
        final Set<BlockPos> blocks = new HashSet<>();
        final Set<BlockPos> spaces = new HashSet<>();
        final Set<BlockPos> unloaded = new HashSet<>();
        final Set<BlockPos> rejected = new HashSet<>();
        final Random random = new Random(7);
        int seedQueries;

        /** 检查模拟区块是否可访问。 */
        public boolean available(BlockPos pos) { return !unloaded.contains(pos); }

        /** 未加载位置禁止读取，并记录搜索开销供预算测试使用。 */
        public boolean isSeed(BlockPos pos) {
            check(available(pos), "不能读取未加载位置");
            seedQueries++;
            return blocks.contains(pos);
        }

        /** 只有明确声明为空位的格子才允许作为候选。 */
        public boolean canPlace(BlockPos pos) { return spaces.contains(pos) && !blocks.contains(pos); }

        /** 模拟世界拒绝放置；拒绝不应被算法算作新增。 */
        public boolean place(BlockPos pos) {
            return !rejected.contains(pos) && canPlace(pos) && blocks.add(pos.immutable());
        }

        /** 固定种子的随机源使失败可以重复。 */
        public int nextInt(int bound) { return random.nextInt(bound); }
    }

    /** 创建精确数量、无种子搜索偏移的蔓延配置。 */
    private static BlockSpreadSpec spec(BlockSpreadSpec.Shape shape, BlockSpreadSpec.Pattern pattern, int count) {
        return new BlockSpreadSpec(BLOCK, count, count, shape, pattern,
                BlockSpreadSpec.Replacement.AIR_ONLY, false, 0, 32);
    }

    /** 构造包含一个种子和指定空位的测试世界。 */
    private static Grid grid(BlockPos... spaces) {
        var world = new Grid();
        world.blocks.add(ORIGIN);
        world.spaces.addAll(Set.of(spaces));
        return world;
    }

    /** 校验语义结果，失败时直接报告对应边界。 */
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    /** 覆盖方向模式、分支策略、数量、失败放置、未知种子、加载范围和遍历预算。 */
    public static void run() {
        var horizontal = grid(ORIGIN.above(), ORIGIN.above(2));
        check(BlockSpreadEngine.spread(horizontal, ORIGIN, spec(BlockSpreadSpec.Shape.HORIZONTAL_2D,
                BlockSpreadSpec.Pattern.CHAIN, 2)) == 0, "二维蔓延不能向上生长");
        var vertical = grid(ORIGIN.above(), ORIGIN.above(2));
        check(BlockSpreadEngine.spread(vertical, ORIGIN, spec(BlockSpreadSpec.Shape.VOLUME_3D,
                BlockSpreadSpec.Pattern.CHAIN, 2)) == 2, "三维蔓延应沿竖直方向继续");

        var chain = grid(ORIGIN.north(), ORIGIN.south());
        check(BlockSpreadEngine.spread(chain, ORIGIN, spec(BlockSpreadSpec.Shape.HORIZONTAL_2D,
                BlockSpreadSpec.Pattern.CHAIN, 2)) == 1, "链式走入死路后应结束");
        var frontier = grid(ORIGIN.north(), ORIGIN.south());
        check(BlockSpreadEngine.spread(frontier, ORIGIN, spec(BlockSpreadSpec.Shape.HORIZONTAL_2D,
                BlockSpreadSpec.Pattern.FRONTIER, 2)) == 2, "边缘扩张应能从另一分支继续");

        var rejected = grid(ORIGIN.north(), ORIGIN.south());
        rejected.rejected.add(ORIGIN.north());
        check(BlockSpreadEngine.spread(rejected, ORIGIN, spec(BlockSpreadSpec.Shape.HORIZONTAL_2D,
                BlockSpreadSpec.Pattern.FRONTIER, 2)) == 1, "放置失败不能计入新增数量");
        check(rejected.blocks.size() == 2, "只应保留种子和一次成功放置");

        var unloaded = grid(ORIGIN.north());
        unloaded.unloaded.add(ORIGIN.north());
        check(BlockSpreadEngine.spread(unloaded, ORIGIN, spec(BlockSpreadSpec.Shape.HORIZONTAL_2D,
                BlockSpreadSpec.Pattern.CHAIN, 1)) == 0, "不能越过未加载范围放置");
        var empty = new Grid();
        empty.spaces.add(ORIGIN);
        check(BlockSpreadEngine.spread(empty, ORIGIN, spec(BlockSpreadSpec.Shape.VOLUME_3D,
                BlockSpreadSpec.Pattern.FRONTIER, 1)) == 0, "没有已有群落时不能凭空播种");

        var bounded = grid();
        for (int x = 1; x < 100; x++) bounded.blocks.add(ORIGIN.east(x));
        var limited = new BlockSpreadSpec(BLOCK, 1, 1, BlockSpreadSpec.Shape.HORIZONTAL_2D,
                BlockSpreadSpec.Pattern.CHAIN, BlockSpreadSpec.Replacement.AIR_ONLY, false, 0, 2);
        BlockSpreadEngine.spread(bounded, ORIGIN, limited);
        check(bounded.seedQueries < 30, "群落遍历必须遵守访问预算");
        check(BlockSpreadEngine.spread(grid(ORIGIN.north()), ORIGIN,
                spec(BlockSpreadSpec.Shape.HORIZONTAL_2D, BlockSpreadSpec.Pattern.CHAIN, 0)) == 0,
                "零数量不能放置方块");

        boolean invalid = false;
        try { new BlockSpreadSpec(BLOCK, 3, 2, BlockSpreadSpec.Shape.HORIZONTAL_2D,
                BlockSpreadSpec.Pattern.CHAIN, BlockSpreadSpec.Replacement.AIR_ONLY, true, 2, 32); }
        catch (IllegalArgumentException expected) { invalid = true; }
        check(invalid, "数量区间倒置必须拒绝");
        System.out.println("Generic block spread regression checks passed");
    }
}
