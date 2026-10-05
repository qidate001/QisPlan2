package com.qidate.qisplan2.ghost.domain.type.mist;

import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainBehavior;
import net.minecraft.server.level.ServerLevel;

/**
 * ========================================
 * 鬼雾鬼域行为
 * ========================================
 *
 * 当前阶段：
 *
 * 仅作为鬼雾鬼域的行为载体。
 *
 * 暂不加入任何鬼域效果。
 *
 * 后续所有“进入鬼雾鬼域之后发生什么”
 * 的规则，都可以逐步放在这里。
 */
public final class GhostMistDomainBehavior
        implements GhostDomainBehavior {

    @Override
    public void onCreate(
            ServerLevel level,
            GhostDomain domain
    ) {
    }

    @Override
    public void tick(
            ServerLevel level,
            GhostDomain domain
    ) {
    }

    @Override
    public void onRemove(
            ServerLevel level,
            GhostDomain domain
    ) {
    }
}