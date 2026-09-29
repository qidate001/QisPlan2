package com.qidate.qisplan2.client.curse;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端诅咒注册。
 */
public final class CurseClientBootstrap {

    private CurseClientBootstrap() {
    }

    /**
     * 注册所有客户端诅咒类型。
     */
    public static void register() {

        /*
         * ============================================================
         * 鬼墓碑
         * ============================================================
         */

        CurseClientRegistry.register(
                ResourceLocation.fromNamespaceAndPath(
                        QisPlan2.MODID,
                        "ghost_tombstone"
                ),
                new CurseClientRegistry.CurseClientType(
                        null,
                        true
                )
        );

        /*
         * ============================================================
         * 鬼签诅咒：生签
         * ============================================================
         */

        CurseClientRegistry.register(
                ResourceLocation.fromNamespaceAndPath(
                        QisPlan2.MODID,
                        "ghost_divination"
                ),
                new CurseClientRegistry.CurseClientType(
                        ResourceLocation.fromNamespaceAndPath(
                                QisPlan2.MODID,
                                "textures/item/life_sign.png"
                        ),
                        true
                )
        );
    }
}