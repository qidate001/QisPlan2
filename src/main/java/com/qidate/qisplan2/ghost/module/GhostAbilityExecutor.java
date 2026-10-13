package com.qidate.qisplan2.ghost.module;

import java.util.Objects;

/**
 * 统一的厉鬼能力执行器。
 *
 * 负责检查能力的执行条件，并调用能力执行逻辑。
 * 如果模块在执行过程中被移除，则跳过尚未开始的能力执行。
 */
public final class GhostAbilityExecutor {

    private GhostAbilityExecutor() {
    }

    /**
     * 尝试执行能力。
     *
     * @return true 表示能力的 execute 方法已被调用；
     *         false 表示能力未执行。
     */
    public static boolean tryExecute(
            GhostAbilityContext context
    ) {
        Objects.requireNonNull(context, "context");

        // 能力执行前，模块必须仍然存在。
        if (!context.isModuleAvailable()) {
            return false;
        }

        GhostAbility ability = context.getAbility();

        if (!ability.canExecute(context)) {
            return false;
        }

        // 前置条件判断期间，模块可能已被移除。
        if (!context.isModuleAvailable()) {
            return false;
        }

        ability.execute(context);
        return true;
    }
}