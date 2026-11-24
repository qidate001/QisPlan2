package com.qidate.qisplan2.ghost.module.environment;

import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.List;

/** 每个模块声明自己的行为工厂；公共框架只调度返回的行为，不维护厉鬼行为枚举表。 */
@FunctionalInterface
public interface GhostBehavior {
    GhostBehavior NONE=context->List.of();
    List<Goal> create(GhostModuleContext context);
}
