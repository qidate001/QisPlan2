package com.qidate.qisplan2.ghost.module;

import com.qidate.qisplan2.ghost.module.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityExecutor;
import com.qidate.qisplan2.ghost.module.data.GhostModuleData;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluence;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceSession;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

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
        ResourceLocation ruleId,
        GhostInfluenceSession influenceSession
) {

    public GhostRuleContext(
            GhostModuleHost host,
            ResourceLocation moduleId,
            ResourceLocation ruleId
    ) {
        this(host, moduleId, ruleId, null);
    }

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
     * 当前规则是否处于统一灵异影响裁决会话中。
     */
    public boolean hasInfluenceSession() {
        return influenceSession != null
                && !influenceSession.isFinished();
    }

    /**
     * 获取当前模块在本次事件中的稳定来源标识。
     */
    public UUID getInfluenceSourceId() {
        if (!hasInfluenceSession()) {
            throw new IllegalStateException(
                    "No active ghost influence session"
            );
        }

        return influenceSession.getSourceId(host, moduleId);
    }


    /**
     * 提交一条灵异影响及其裁决通过后的效果。
     *
     * 默认不执行额外的死机清理操作。
     */
    public void submitInfluence(
            UUID targetId,
            GhostInfluenceType type,
            double intensity,
            Runnable acceptedEffect
    ) {
        submitInfluence(
                targetId,
                type,
                intensity,
                acceptedEffect,
                () -> {
                }
        );
    }

    /**
     * 提交一条灵异影响及其后续效果。
     *
     * @param targetId 影响目标
     * @param type 影响类型
     * @param intensity 影响强度
     * @param acceptedEffect 裁决通过后执行的效果
     * @param deadlockedEffect 该影响进入死机状态后执行的清理操作
     */
    public void submitInfluence(
            UUID targetId,
            GhostInfluenceType type,
            double intensity,
            Runnable acceptedEffect,
            Runnable deadlockedEffect
    ) {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(acceptedEffect, "acceptedEffect");
        Objects.requireNonNull(deadlockedEffect, "deadlockedEffect");

        if (!hasInfluenceSession()) {
            throw new IllegalStateException(
                    "Cannot submit influence without an active session"
            );
        }

        GhostInfluence influence = new GhostInfluence(
                host,
                moduleId,
                ruleId,
                targetId,
                type,
                intensity,
                getInfluenceSourceId(),
                influenceSession.getBatchId()
        );

        influenceSession.submit(
                influence,
                acceptedEffect,
                deadlockedEffect
        );
    }

    /**
     * 提交一个灵异影响，并在裁决通过后执行指定能力。
     *
     * 能力准备失败时，不提交影响。
     *
     * @return 是否成功提交影响
     */
    public boolean submitAbilityInfluence(
            ResourceLocation abilityId,
            GhostEvent event,
            UUID targetId,
            GhostInfluenceType type,
            double intensity
    ) {
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(type, "type");

        if (!hasInfluenceSession()) {
            return false;
        }

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

        Runnable deferredAbility =
                GhostAbilityExecutor.prepareDeferred(abilityContext);

        if (deferredAbility == null) {
            return false;
        }

        submitInfluence(
                targetId,
                type,
                intensity,
                deferredAbility
        );

        return true;
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