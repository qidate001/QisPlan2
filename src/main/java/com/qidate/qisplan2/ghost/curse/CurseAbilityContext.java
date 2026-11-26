package com.qidate.qisplan2.ghost.curse;

import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import com.qidate.qisplan2.ghost.module.influence.GhostAttackContext;
import java.util.UUID;

/** 持续诅咒能力的语义上下文。无宿主引用，强度和对象来自独立诅咒实例。 */
public record CurseAbilityContext(UUID instance, UUID target, double strength, GhostEvent event,
        GhostAttackContext incoming, Runnable remove, Runnable advance) {}
