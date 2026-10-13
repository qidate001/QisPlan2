package com.qidate.qisplan2.ghost.module;

import java.util.List;
import java.util.Objects;

/**
 * 统一的厉鬼能力执行器。
 *
 * 负责检查模块有效性、媒介限制和能力执行条件，
 * 并在全部条件满足后执行能力。
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

        // 1. 模块必须仍然存在。
        if (!context.isModuleAvailable()) {
            return false;
        }

        GhostModule module =
                GhostModuleRegistry.get(context.moduleId());

        if (module == null) {
            return false;
        }

        // 2. 检查该模块声明的全部媒介。
        // 任意媒介不允许本次调用，就禁止执行能力。
        List<GhostMedium> media =
                List.copyOf(module.getMedia());

        for (GhostMedium medium : media) {
            if (!context.isModuleAvailable()) {
                return false;
            }

            if (!medium.allows(context)) {
                return false;
            }
        }

        // 3. 获取能力定义并检查执行条件。
        if (!context.isModuleAvailable()) {
            return false;
        }

        GhostAbility ability = context.getAbility();

        if (!ability.canExecute(context)) {
            return false;
        }

        // 4. 前置条件判断期间，模块也可能被移除。
        if (!context.isModuleAvailable()) {
            return false;
        }

        // 5. 执行能力。
        ability.execute(context);
        return true;
    }
}