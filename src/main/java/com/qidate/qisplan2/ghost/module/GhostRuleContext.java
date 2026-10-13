package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
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
}