package com.qidate.qisplan2.ghost.module.host.player.binding;

import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;

/** 玩家生命周期与表现的适配协议；模块定义不依赖此协议。 */
public interface GhostPlayerBinding {
    /**
     * 处理刚完成驾驭后的载体初始化；默认实现不额外操作。
     */
    default void onPossess(PlayerGhostModuleContext context) {}

    /**
     * 推进玩家载体上的持续状态或表现，持久数据仍通过模块入口保存。
     */
    default void onPlayerTick(PlayerGhostModuleContext context) {}

    /**
     * 默认移除当前玩家来源、当前模块类型的运行时鬼域。
     * 仅释放运行时资源，不修改持久化开关或成长数据。
     */
    default void onPlayerSuspended(PlayerGhostModuleContext context) {
        GhostDomainManager.get(context.player().serverLevel())
                .removeBySourceAndType(context.player().getUUID(), context.ghost());
    }

    /**
     * 处理解除驾驭后的表现或鬼域清理。
     */
    default void onRelease(PlayerGhostModuleContext context) {}

}
