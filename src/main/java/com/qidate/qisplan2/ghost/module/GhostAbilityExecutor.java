package com.qidate.qisplan2.ghost.module;

import java.util.Objects;

/**
 * 灵异能力执行器。
 *
 * 统一负责能力的查找、执行条件检查与执行。
 *
 * 规则触发和驭鬼者主动调用，
 * 最终都应通过此执行器执行能力。
 */
public final class GhostAbilityExecutor {

    private GhostAbilityExecutor() {
    }

    /**
     * 尝试执行指定上下文对应的能力。
     *
     * @param context 本次能力调用的上下文
     * @return true 表示能力条件满足且执行完成；
     *         false 表示能力未执行
     */
    public static boolean tryExecute(
            GhostAbilityContext context
    ) {
        Objects.requireNonNull(context, "context");

        GhostAbility ability = context.getAbility();

        // 检查能力自身的执行条件。
        if (!ability.canExecute(context)) {
            return false;
        }

        // 执行具体能力效果。
        ability.execute(context);

        return true;
    }
}