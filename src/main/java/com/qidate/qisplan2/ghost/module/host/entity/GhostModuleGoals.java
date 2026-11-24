package com.qidate.qisplan2.ghost.module.host.entity;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.host.ModuleGhostDomains;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.List;

/** Goal 的实例、控制标记与物理生命周期只由实体适配层管理。 */
public final class GhostModuleGoals {
    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostModuleGoals() {}

    /**
     * 调用模块自己声明的工厂创建行为列表，不维护具体厉鬼的选择表。
     * 未声明实体行为时返回空列表，调度与强度竞争由实体宿主负责。
     */
    public static List<Goal> goals(GhostModule module, GhostModuleHost host) {
        return module.behavior().create(new GhostModuleContext(host, module.getId()));
    }

    /**
     * 释放该模块在实体上的运行时外部资源。
     * 模块卸载或实体离开世界时调用，不删除模块的持久成长数据。
     */
    public static void detach(GhostModule module, GhostModuleHost host) {
        ModuleGhostDomains.closeRuntime(host, module.getId());
    }
}
