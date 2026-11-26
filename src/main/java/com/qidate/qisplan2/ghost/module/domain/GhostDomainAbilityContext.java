package com.qidate.qisplan2.ghost.module.domain;

import com.qidate.qisplan2.ghost.module.environment.GhostWorldAccess;
import com.qidate.qisplan2.ghost.module.event.GhostEvent;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;

/** 鬼域内部能力只读取覆盖事实与世界操作端口，无法取得来源的宿主类型。 */
public record GhostDomainAbilityContext(UUID domain, LivingEntity target, int grade,
        double strength, GhostEvent event, GhostWorldAccess world) {}
