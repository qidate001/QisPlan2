package com.qidate.qisplan2.ghost.module.host.entity;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostRuleRuntime;
import com.qidate.qisplan2.ghost.module.host.ModuleGhostDomains;
import com.qidate.qisplan2.ghost.module.GhostRuleTask;

import java.util.List;

/** 从模块规则声明创建当前实例的持续任务；不包含具体厉鬼的行为分发表。 */
public final class GhostModuleGoals {
    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostModuleGoals() {}

    /**
     * 调用模块规则声明的任务工厂，不再维护独立的 behavior 入口。
     * 未声明实体行为时返回空列表，调度与强度竞争由实体宿主负责。
     */
    public static List<GhostRuleTask> goals(GhostModule module, GhostModuleHost host) {
        return module.getRules().stream()
                .flatMap(rule -> rule.createTasks(GhostRuleRuntime.services(host,module.getId(),rule.getId())).stream())
                .toList();
    }

    /**
     * 释放该模块在实体上的运行时外部资源。
     * 模块卸载或实体离开世界时调用，不删除模块的持久成长数据。
     */
    public static void detach(GhostModule module, GhostModuleHost host) {
        ModuleGhostDomains.closeRuntime(host, module.getId());
    }
}
