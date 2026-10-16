package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.module.GhostRuleContext;
import net.minecraft.server.MinecraftServer;

import java.util.Objects;
import java.util.UUID;

/**
 * 通用灵异影响收集器。
 *
 * <p>
 * 负责收集目标身上由诅咒提供的灵异影响，
 * 并将其提交到当前规则的影响裁决会话。
 *
 * <p>
 * 本类不负责冲突裁决，也不直接执行影响效果。
 */
public final class GhostInfluenceCollector {

    private GhostInfluenceCollector() {
    }

    /**
     * 收集目标身上所有有效、能够提供灵异影响的诅咒。
     *
     * @param context 当前规则上下文
     * @param target  影响目标
     * @param server  当前服务器
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

            context.submitInfluence(
                    target,
                    provider.getInfluenceType(),
                    provider.getInfluenceIntensity(),
                    () -> {
                    }
            );

            submitted++;
        }

        return submitted;
    }
}