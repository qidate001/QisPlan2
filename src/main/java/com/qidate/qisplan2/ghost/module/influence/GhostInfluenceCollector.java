package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.Objects;
import java.util.UUID;

/**
 * 通用灵异影响收集器。
 *
 * 负责收集目标身上所有有效、能够提供灵异影响的诅咒，
 * 并提交到当前规则的影响裁决会话。
 *
 * 不负责裁决，也不直接执行保护或袭击效果。
 */
public final class GhostInfluenceCollector {

    private GhostInfluenceCollector() {
    }

    /**
     * 收集目标身上所有有效、能够提供灵异影响的诅咒。
     *
     * 诅咒参与死机时，注销对应的具体诅咒实例。
     *
     * @param context 当前规则上下文
     * @param target 影响目标
     * @param server 当前服务器
     * @return 成功提交的影响数量
     */
    public static int collectCurses(
            GhostRuleContext context,
            UUID target,
            MinecraftServer server
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(server, "server");

        if (!context.hasInfluenceSession()) {
            return 0;
        }

        int submitted = 0;

        for (Curse curse : CurseManager.getByTarget(target)) {
            if (!curse.isValid(server)) {
                continue;
            }

            if (!(curse instanceof GhostInfluenceProvider provider)) {
                continue;
            }

            submitCurse(context.influenceSession(), context.host(), context.moduleId(), context.ruleId(),
                    target, curse, provider, () -> CurseManager.remove(server, curse.getId()));

            submitted++;
        }

        return submitted;
    }

    /**
     * 主动能力的诅咒收集入口。直接使用裁决会话，不要求存在或触发某条规则。
     */
    public static void collectCurses(GhostInfluenceSession session, GhostModuleHost host,
            ResourceLocation moduleId, ResourceLocation abilityId, UUID target, MinecraftServer server) {
        for (Curse curse : CurseManager.getByTarget(target)) {
            if (!curse.isValid(server) || !(curse instanceof GhostInfluenceProvider provider)) continue;
            submitCurse(session, host, moduleId, abilityId, target, curse, provider,
                    () -> CurseManager.remove(server, curse.getId()));
        }
    }
    /** 保留保护的模块来源；其他模块的保护使用独立来源，不错误地使来袭模块死机。 */
    static void submitCurse(GhostInfluenceSession session, GhostModuleHost host,
            ResourceLocation moduleId, ResourceLocation abilityId, UUID target, Curse curse,
            GhostInfluenceProvider provider, Runnable cleanup) {
        if (!session.claimCurse(curse.getId())) return;
        UUID source = moduleId.equals(provider.influenceModuleId())
                ? session.getSourceId(host, moduleId) : curse.getId();
        session.submit(new GhostInfluence(host, moduleId, abilityId, target, provider.getInfluenceType(),
                provider.getInfluenceIntensity(), source, session.getBatchId()), () -> {}, cleanup);
    }
}