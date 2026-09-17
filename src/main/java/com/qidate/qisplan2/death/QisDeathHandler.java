package com.qidate.qisplan2.death;

import com.qidate.qisplan2.ghost.domain.type.eye.GhostEyeDomainController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class QisDeathHandler {

    private QisDeathHandler() {
    }

    /**
     * 强制死亡，并执行 QisPlan2 的死亡清理。
     *
     * <p>适用于 QisPlan2 主动执行的特殊死亡。</p>
     */
    public static void forceKillAndCleanup(
            LivingEntity entity
    ) {

        if (!entity.isAlive()) {
            return;
        }

        cleanup(entity);

        forceKill(entity);
    }

    /**
     * 强制死亡，但不执行额外清理。
     *
     * <p>不会触发原版死亡事件。</p>
     */
    public static void forceKill(
            LivingEntity entity
    ) {

        if (!entity.isAlive()) {
            return;
        }

        entity.setHealth(0.0F);
    }

    /**
     * 执行 QisPlan2 的死亡清理。
     *
     * <p>不会改变实体生命值。</p>
     *
     * <p>该方法必须保持幂等：
     * 重复调用不会产生额外副作用。</p>
     */
    public static void cleanup(
            LivingEntity entity
    ) {

        /*
         * 玩家死亡时关闭鬼眼。
         */
        if (entity instanceof ServerPlayer player) {

            GhostEyeDomainController.close(player);
        }
    }
}