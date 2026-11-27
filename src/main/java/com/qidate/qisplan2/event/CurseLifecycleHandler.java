package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.network.curse.CurseNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** 将已确认的死亡与重生接入通用诅咒生命周期，具体保留策略由诅咒声明。 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class CurseLifecycleHandler {
    private CurseLifecycleHandler() {}

    /**
     * 掉落阶段已通过可取消的死亡事件，避免被救下的目标丢失诅咒。
     * 原版即使开启死亡不掉落、关闭生物战利品或没有掉落物，仍经过此阶段。
     * 取消掉落只影响物品生成，不撤销死亡，因此也需要接收已取消的掉落事件。
     */
    @SubscribeEvent(receiveCanceled = true)
    public static void onConfirmedDeath(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (!entity.level().isClientSide && entity.getServer() != null) {
            CurseManager.onTargetDeath(entity.getServer(), entity.getUUID());
        }
    }

    /** 死亡重建的兜底清理；末地返回等非死亡 Clone 不改变诅咒。 */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) {
            CurseManager.onTargetDeath(player.getServer(), player.getUUID());
        }
    }

    /** 玩家重生替换实体后重新发送保留的诅咒；实例仍通过原目标 UUID 关联。 */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CurseNetwork.sync(player);
    }
}
