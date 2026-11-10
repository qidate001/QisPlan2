package com.qidate.qisplan2.ghost.module.host.entity;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.module.GhostModule;
import java.util.List;
import com.qidate.qisplan2.ghost.module.implementation.mist.GhostMistModule;
import net.minecraft.world.entity.ai.goal.Goal;

/** Goal 的实例、控制标记与物理生命周期只由实体适配层管理。 */
public final class GhostEntityBindings {
    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
    private GhostEntityBindings() {}

    /**
     * 根据模块声明的行为类型创建实体行为列表。
     * 未声明实体行为时返回空列表，调度与强度竞争由实体宿主负责。
     */
    public static List<Goal> goals(GhostModule module, ModularGhostEntity host) {
        return switch (module.behavior()) {
            case NIGHT_HUNT -> new NightWandererEntityBinding().createEntityGoals(host);
            case KNOCK_DOORS -> new KnockingGhostEntityBinding().createEntityGoals(host);
            case FOLLOW_CALL -> new CallingGhostEntityBinding().createEntityGoals(host);
            case WANDER -> new GhostMistEntityBinding().createEntityGoals(host);
            default -> List.of();
        };
    }

    /**
     * 驱动已注册的实体生命周期适配。
     * 当前为鬼雾维护鬼域；这类资源维护不占用 Goal 的移动控制。
     */
    public static void tick(GhostModule module, ModularGhostEntity host) {
        if (module.getId().equals(GhostMistModule.ID))
            new GhostMistEntityBinding().onEntityTick(host);
    }

    /**
     * 释放该模块在实体上的运行时外部资源。
     * 模块卸载或实体离开世界时调用，不删除模块的持久成长数据。
     */
    public static void detach(GhostModule module, ModularGhostEntity host) {
        if (module.getId().equals(GhostMistModule.ID))
            new GhostMistEntityBinding().onEntityDetached(host);
    }
}
