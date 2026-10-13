package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 单次规则执行的上下文。
 *
 * 保存当前模块宿主与规则 ID。
 * 通过现有 GhostModuleContext 访问模块实例数据。
 *
 * 当前阶段不负责规则状态持久化，
 * 也不直接执行能力。
 */
public record GhostRuleContext(
        GhostModuleHost host,
        ResourceLocation moduleId,
        ResourceLocation ruleId
) {

    public GhostRuleContext {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(ruleId, "ruleId");

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

        boolean ruleExists = module.getRules().stream()
                .anyMatch(rule -> rule.getId().equals(ruleId));

        if (!ruleExists) {
            throw new IllegalArgumentException(
                    "Module does not define rule: "
                            + moduleId + "/" + ruleId
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
     * 获取当前规则定义。
     */
    public GhostRule getRule() {
        GhostModule module = Objects.requireNonNull(
                GhostModuleRegistry.get(moduleId),
                "Unknown ghost module: " + moduleId
        );

        return module.getRules().stream()
                .filter(rule -> rule.getId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Rule definition disappeared: "
                                + moduleId + "/" + ruleId
                ));
    }

    /**
     * 获取当前模块实例的实时数据。
     */
    public GhostModuleData.Entry getModuleData() {
        return getModuleContext().getData();
    }

    /**
     * 请求执行当前模块定义的能力。
     *
     * @param abilityId 当前模块内的能力 ID
     * @param event     触发当前规则的语义事件
     * @return true 表示能力执行方法正常返回
     */
    public boolean tryExecuteAbility(
            ResourceLocation abilityId,
            GhostEvent event
    ) {
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(event, "event");

        // 当前模块已被移除，不再尝试创建能力上下文。
        if (!host.hasModule(moduleId)) {
            return false;
        }

        GhostAbilityContext abilityContext =
                new GhostAbilityContext(
                        host,
                        moduleId,
                        abilityId,
                        GhostAbilityContext.Source.RULE,
                        event
                );

        return GhostAbilityExecutor.tryExecute(abilityContext);
    }
}