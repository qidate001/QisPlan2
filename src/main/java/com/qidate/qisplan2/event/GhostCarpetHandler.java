package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.TouchEvent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** 静态地毯的接触桥；每个生命实体每 tick 最多报告一次真实地毯接触。 */
@EventBusSubscriber(modid = QisPlan2.MODID)
public final class GhostCarpetHandler {
    /** 事件由总线调用，不创建处理器实例。 */
    private GhostCarpetHandler() {}

    /**
     * 沿用脚下或当前位置的地毯检测；同时踩到相邻地毯也只计一个 tick。
     * 离开时不发布采样、不清空目标关系数据；死亡和客户端不进入规则系统。
     */
    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || !target.isAlive()
                || target.level().isClientSide()) return;
        var pos = target.blockPosition();
        var state = target.level().getBlockState(pos);
        if (!state.is(ModBlocks.GHOST_CARPET.get())) state = target.level().getBlockState(pos.below());
        if (state.is(ModBlocks.GHOST_CARPET.get()) && state.getBlock() instanceof GhostModuleHost host)
            GhostModuleRuntime.dispatch(host, TouchEvent.sample(null, target, false));
    }
}
