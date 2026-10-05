package com.qidate.qisplan2.ghost.isolation;

import com.qidate.qisplan2.block.GhostDoorBlock;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.core.ModTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 灵异隔绝规则。
 *
 * <p>
 * 用于统一判断某个方块状态是否能够阻断灵异传播。
 * </p>
 *
 * <p>
 * 普通方块通过 {@link ModTags.Blocks#GHOST_ISOLATION} 判断；
 * 需要根据 BlockState 判断的特殊情况则通过自定义规则处理。
 * </p>
 */
public final class GhostIsolationBlockRegistry {

    private static final List<Predicate<BlockState>> SPECIAL_RULES =
            new ArrayList<>();

    private GhostIsolationBlockRegistry() {
    }

    /**
     * 初始化特殊的灵异隔绝规则。
     */
    public static void init() {

        // 只有关闭状态的鬼门能够隔绝灵异。
        register(state ->
                state.is(ModBlocks.GHOST_DOOR)
                        && !state.getValue(GhostDoorBlock.OPEN)
        );
    }

    /**
     * 注册一个自定义的灵异隔绝规则。
     *
     * <p>
     * 适用于无法直接通过 Tag 表达的 BlockState 条件。
     * </p>
     */
    public static void register(
            Predicate<BlockState> rule
    ) {

        SPECIAL_RULES.add(rule);
    }

    /**
     * 判断一个方块状态是否能够阻断灵异传播。
     */
    public static boolean canBlockSupernatural(
            BlockState state
    ) {

        /*
         * 普通隔绝方块：
         * 直接通过 Tag 判断。
         */
        if (state.is(ModTags.Blocks.GHOST_ISOLATION)) {
            return true;
        }

        /*
         * 特殊 BlockState 规则。
         */
        for (Predicate<BlockState> rule : SPECIAL_RULES) {

            if (rule.test(state)) {
                return true;
            }
        }

        return false;
    }
}