package com.qidate.qisplan2.ghost.module.ability;

import com.qidate.qisplan2.ghost.module.influence.GhostInfluence;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceCollector;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceSession;
import com.qidate.qisplan2.ghost.module.influence.GhostInfluenceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** 能力声明的主动影响：描述目标、类型和实际强度，不识别调用宿主类型。 */
public record GhostAbilityInfluence(UUID target, GhostInfluenceType type, double intensity,
        MinecraftServer server) {
    /** 从能力已经解析出的作用目标创建声明；无有效服务端目标时不创建。 */
    public static GhostAbilityInfluence of(LivingEntity target, GhostInfluenceType type, double intensity) {
        return target == null || target.getServer() == null ? null
                : new GhostAbilityInfluence(target.getUUID(), type, intensity, target.getServer());
    }

    /** 收集目标上的持续影响，再统一裁决；只有获准回调可以执行能力效果。 */
    public boolean execute(GhostAbilityContext context, BooleanSupplier effect) {
        var session = new GhostInfluenceSession();
        if (server != null) GhostInfluenceCollector.collectCurses(session, context.host(), context.moduleId(),
                context.abilityId(), target, server);
        return resolve(session, context, effect);
    }

    /** 可向已有影响会话加入本次能力并完成裁决，返回实际效果结果。 */
    public boolean resolve(GhostInfluenceSession session, GhostAbilityContext context, BooleanSupplier effect) {
        boolean[] result = {false};
        session.submit(new GhostInfluence(context.host(), context.moduleId(), context.abilityId(), target,
                type, intensity, session.getSourceId(context.host(), context.moduleId()), session.getBatchId()),
                () -> result[0] = effect.getAsBoolean());
        session.resolveAndApply();
        return result[0];
    }
}
