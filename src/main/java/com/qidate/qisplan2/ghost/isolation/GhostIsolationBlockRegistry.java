package com.qidate.qisplan2.ghost.isolation;

import com.qidate.qisplan2.core.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 灵异隔绝方块注册表。
 *
 * <p>
 * 用于统一判断某个方块状态是否能够阻断灵异传播。
 * </p>
 */
public final class GhostIsolationBlockRegistry {

    private static final List<Predicate<BlockState>> RULES =
            new ArrayList<>();

    private GhostIsolationBlockRegistry() {
    }

    public static void init() {

        register(
                ModBlocks.GHOST_STONE_BRICKS
        );

        register(
                Blocks.GOLD_BLOCK.builtInRegistryHolder()
        );
    }

    /**
     * 注册一个能够隔绝灵异的方块。
     *
     * <p>
     * 使用 Holder 而不是直接获取 Block，
     * 避免在 DeferredRegister 完成绑定前调用 get()。
     * </p>
     */
    public static void register(
            Holder<Block> block
    ) {

        RULES.add(
                state -> state.is(block)
        );
    }

    /**
     * 注册一个自定义的灵异隔绝规则。
     *
     * <p>
     * 适用于需要根据 BlockState 判断的特殊方块，
     * 例如只有关闭状态的鬼门才能隔绝灵异。
     * </p>
     */
    public static void register(
            Predicate<BlockState> rule
    ) {

        RULES.add(rule);
    }

    /**
     * 判断一个方块状态是否能够阻断灵异传播。
     */
    public static boolean canBlockSupernatural(
            BlockState state
    ) {

        for (Predicate<BlockState> rule : RULES) {

            if (rule.test(state)) {
                return true;
            }
        }

        return false;
    }
}