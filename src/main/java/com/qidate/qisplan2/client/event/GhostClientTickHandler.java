package com.qidate.qisplan2.client.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.client.curse.ClientCurseState;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 齐计划2统一客户端 Tick。
 *
 * <p>
 * 所有需要在客户端每 Tick 执行的系统，
 * 都统一从这里进入。
 * </p>
 */
@EventBusSubscriber(
        modid = QisPlan2.MODID
)
public final class GhostClientTickHandler {

    private GhostClientTickHandler() {
    }

    /**
     * 客户端 Tick。
     */
    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        /*
         * 单人游戏暂停时，
         * 服务端游戏逻辑也会暂停。
         *
         * 客户端预测状态必须与服务端游戏时间保持一致，
         * 因此暂停期间不能继续 Tick。
         */
        if (minecraft.isPaused()) {
            return;
        }

        /*
         * 客户端诅咒状态 Tick。
         *
         * 客户端只负责预测剩余持续时间，
         * 不参与诅咒实际逻辑。
         */
        ClientCurseState.tick();
    }
}