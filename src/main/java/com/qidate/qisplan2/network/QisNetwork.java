package com.qidate.qisplan2.network;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.network.divinationslip.GhostDivinationNetwork;
import com.qidate.qisplan2.network.ghostdoor.GhostDoorNetwork;
import com.qidate.qisplan2.network.ghostdomain.GhostDomainNetwork;
import com.qidate.qisplan2.network.ghostdoor.GhostDoorPlateNetwork;
import com.qidate.qisplan2.network.ghosteye.GhostEyeNetwork;
import com.qidate.qisplan2.network.ghostpiano.GhostPianoNetwork;
import com.qidate.qisplan2.network.payload.*;
import com.qidate.qisplan2.network.possession.GhostPossessionNetwork;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(
        modid = QisPlan2.MODID
)
public final class QisNetwork {

    private QisNetwork() {
    }

    @SubscribeEvent
    public static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {

        var registrar =
                event.registrar("1");

        /*
         * ========================================================
         * 鬼钢琴
         * ========================================================
         */

        GhostPianoNetwork.register(event);

        /*
         * ========================================================
         * 驾驭小游戏
         * ========================================================
         */

        GhostPossessionNetwork.register(event);

        /*
         * ========================================================
         * 鬼门
         * ========================================================
         */

        GhostDoorNetwork.register(event);

        /*
         * ========================================================
         * 鬼门牌
         * ========================================================
         */

        GhostDoorPlateNetwork.register(event);

        /*
         * ========================================================
         * 鬼域
         * ========================================================
         */

        GhostDomainNetwork.register(event);

        /*
         * ========================================================
         * 鬼签
         * ========================================================
         */

        GhostDivinationNetwork.register(event);

        /*
         * ========================================================
         * 鬼眼
         * ========================================================
         */

        GhostEyeNetwork.register(event);
    }
}