package com.qidate.qisplan2.client;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class GhostUmbrellaClient {

    private GhostUmbrellaClient() {
    }

    public static void registerAdditionalModels(
            ModelEvent.RegisterAdditional event
    ) {
        /*
         * 关闭状态模型
         */
        event.register(
                ModelResourceLocation.standalone(
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "item/ghost_umbrella_closed"
                        )
                )
        );

        /*
         * 打开状态模型
         */
        event.register(
                ModelResourceLocation.standalone(
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "item/ghost_umbrella_open"
                        )
                )
        );
    }
}