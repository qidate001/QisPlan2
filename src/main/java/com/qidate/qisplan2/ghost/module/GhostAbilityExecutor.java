package com.qidate.qisplan2.ghost.module;

import java.util.List;
import java.util.Objects;

/**
 * 统一的厉鬼能力执行器。
 *
 * 负责检查模块有效性、媒介限制和能力执行条件，
 * 并统一处理能力产生的复苏刺激。
 */
public final class GhostAbilityExecutor {

    private GhostAbilityExecutor() {
    }

    /**
     * 尝试执行能力。
     *
     * @return true 表示能力的执行方法已被调用；
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

        // 4. 检查前置条件判断期间模块是否被移除。
        if (!context.isModuleAvailable()) {
            return false;
        }

        // 5. 获取本次能力声明的复苏刺激。
        GhostAbilityStimulation stimulation =
                Objects.requireNonNull(
                        ability.getStimulation(context),
                        "Ability stimulation must not be null"
                );

        GhostAbilityStimulation.Timing timing =
                stimulation.timing();

        // 6. ON_ACCEPTED：能力被接受执行后结算。
        // 在调用能力效果前结算，避免效果执行失败或提前返回
        // 导致已经接受执行的能力无法产生刺激。
        if (timing
                == GhostAbilityStimulation.Timing.ON_ACCEPTED) {
            applyStimulation(context, stimulation);
        }

        // 7. 执行能力并取得效果结果。
        boolean effectSucceeded =
                ability.executeWithResult(context);

        // 8. ON_EFFECT_SUCCESS：只有效果成功才结算。
        if (timing
                == GhostAbilityStimulation.Timing.ON_EFFECT_SUCCESS
                && effectSucceeded) {
            applyStimulation(context, stimulation);
        }

        // MANUAL_CHECKPOINT 暂不自动结算。
        // 后续实现显式检查点机制后再处理。

        // 保持旧有语义：能力执行方法已被调用即返回 true。
        return true;
    }

    /**
     * 统一提交复苏刺激。
     *
     * 模块被移除时不再结算。
     */
    private static boolean applyStimulation(
            GhostAbilityContext context,
            GhostAbilityStimulation stimulation
    ) {
        if (!context.isModuleAvailable()) {
            return false;
        }

        if (stimulation.revivalAmount() <= 0.0D) {
            return true;
        }

        return context.getModuleContext().addRevival(
                stimulation.revivalAmount()
        );
    }
}