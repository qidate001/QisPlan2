package com.qidate.qisplan2.ghost.module.ability;

import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 单次能力调用的上下文。
 *
 * 描述本次能力调用对应的模块、能力、调用来源和事件。
 *
 * 上下文同时提供显式的复苏刺激检查点接口。
 * 检查点本身不直接修改模块数据，而是将结算请求交给
 * 当前执行器创建的执行会话处理。
 *
 * 未绑定执行会话的上下文，其检查点调用不会产生效果。
 */
public record GhostAbilityContext(
        GhostModuleHost host,
        ResourceLocation moduleId,
        ResourceLocation abilityId,
        Source source,
        GhostEvent event,
        Runnable stimulationCheckpoint
) {

    /**
     * 能力调用来源。
     */
    public enum Source {

        /**
         * 由厉鬼规则触发。
         */
        RULE,

        /**
         * 由驭鬼者主动调用。
         */
        POSSESSION
    }

    /**
     * 保留原有五参数构造方式。
     *
     * 通过此构造方式创建的上下文不绑定复苏刺激检查点。
     * 只有执行器创建的执行上下文才能请求检查点结算。
     */
    public GhostAbilityContext(
            GhostModuleHost host,
            ResourceLocation moduleId,
            ResourceLocation abilityId,
            Source source,
            GhostEvent event
    ) {
        this(
                host,
                moduleId,
                abilityId,
                source,
                event,
                () -> {
                    // 未绑定执行会话时，不执行任何结算。
                }
        );
    }

    public GhostAbilityContext {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(
                stimulationCheckpoint,
                "stimulationCheckpoint"
        );

        if (!host.hasModule(moduleId)) {
            throw new IllegalArgumentException(
                    "Host does not contain module: " + moduleId
            );
        }

        GhostModule module = GhostModuleRegistry.get(moduleId);

        if (module == null) {
            throw new IllegalArgumentException(
                    "Unknown ghost module: " + moduleId
            );
        }

        boolean abilityExists = module.getAbilities().stream()
                .anyMatch(ability -> ability.getId().equals(abilityId));

        if (!abilityExists) {
            throw new IllegalArgumentException(
                    "Module does not define ability: "
                            + moduleId + "/" + abilityId
            );
        }
    }

    /**
     * 获取当前模块的上下文。
     */
    public GhostModuleContext getModuleContext() {
        return new GhostModuleContext(host, moduleId);
    }

    /**
     * 当前模块是否仍由宿主持有。
     *
     * 上下文创建后，模块仍可能被移除，
     * 因此不能仅凭上下文创建成功就认定模块一直有效。
     */
    public boolean isModuleAvailable() {
        return host.hasModule(moduleId);
    }

    /**
     * 获取当前能力定义。
     */
    public GhostAbility getAbility() {
        GhostModule module = Objects.requireNonNull(
                GhostModuleRegistry.get(moduleId),
                "Unknown ghost module: " + moduleId
        );

        return module.getAbilities().stream()
                .filter(ability -> ability.getId().equals(abilityId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Ability definition disappeared: "
                                + moduleId + "/" + abilityId
                ));
    }

    /**
     * 请求结算本次能力声明的复苏刺激。
     *
     * 仅对绑定了有效执行会话的上下文有效。
     * 是否允许结算、是否已经结算以及实际增加多少复苏值，
     * 均由执行器负责决定。
     *
     * 对于 MANUAL_CHECKPOINT 能力，同一次调用可以到达多个
     * 检查点，但执行器只会接受第一次有效的结算请求。
     */
    public void checkpointStimulation() {
        stimulationCheckpoint.run();
    }

    /**
     * 创建绑定指定检查点处理器的执行上下文。
     *
     * 保留本次调用原有的宿主、模块、能力、来源和事件。
     * 不修改原上下文，也不共享其他调用的检查点状态。
     *
     * @param checkpoint 本次执行会话的检查点处理器
     * @return 新的执行上下文
     */
    GhostAbilityContext withStimulationCheckpoint(
            Runnable checkpoint
    ) {
        return new GhostAbilityContext(
                host,
                moduleId,
                abilityId,
                source,
                event,
                checkpoint
        );
    }
}