package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * 单次能力调用的上下文。
 *
 * 描述本次能力调用对应的模块、能力和调用来源。
 *
 * 当前阶段只建立上下文结构，
 * 暂不负责驾驭关系验证、能力执行和规则状态判断。
 */
public record GhostAbilityContext(
        GhostModuleHost host,
        ResourceLocation moduleId,
        ResourceLocation abilityId,
        Source source,
        GhostEvent event
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

    public GhostAbilityContext {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(source, "source");

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
}