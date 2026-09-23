package com.qidate.qisplan2.ghost.layer;

import com.qidate.qisplan2.ghost.domain.GhostDomain;
import net.minecraft.world.entity.Entity;

public final class GhostLayerAccess {

    private GhostLayerAccess() {}

    /**
     * 是否能够被这个鬼域拉入。
     *
     * 规则：
     * 鬼域层数 > 肉身鬼域抵抗
     */
    public static boolean canEnter(
            Entity entity,
            GhostDomain domain
    ) {
        return domain.getLayer()
                > GhostResistanceHandler.getResistance(entity);
    }

    /**
     * 当前层数是否允许存在于这个鬼域。
     */
    public static boolean canStayAtLayer(
            Entity entity,
            GhostDomain domain,
            int layer
    ) {
        return canEnter(entity, domain)
                && layer <= domain.getLayer();
    }

    /**
     * 当前鬼域能够容纳的最高层。
     */
    public static int maxLayer(
            Entity entity,
            GhostDomain domain
    ) {
        if (!canEnter(entity, domain)) {
            return 0;
        }

        return domain.getLayer();
    }
}