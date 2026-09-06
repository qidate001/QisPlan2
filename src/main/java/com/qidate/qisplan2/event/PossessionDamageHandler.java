package com.qidate.qisplan2.event;

import com.qidate.qisplan2.death.SupernaturalDamageHelper;
import com.qidate.qisplan2.ghost.PossessionHandler;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class PossessionDamageHandler {

    private PossessionDamageHandler() {
    }


    /**
     * 驭鬼者的非灵异伤害减免。
     *
     * 灵异伤害完全不受影响。
     */
    @SubscribeEvent
    public static void onLivingDamage(
            LivingDamageEvent.Pre event
    ) {

        /*
         * ========================================================
         * 只处理玩家
         * ========================================================
         */

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }


        /*
         * ========================================================
         * 灵异伤害不减免
         * ========================================================
         */

        if (SupernaturalDamageHelper.isSupernatural(
                event.getSource()
        )) {
            return;
        }


        /*
         * ========================================================
         * 获取驭鬼者减伤
         * ========================================================
         */

        double reduction =
                PossessionHandler
                        .getNonSupernaturalDamageReduction(
                                player
                        );


        if (reduction <= 0.0D) {
            return;
        }


        /*
         * ========================================================
         * 计算实际伤害
         * ========================================================
         */

        float oldDamage =
                event.getNewDamage();


        float newDamage =
                (float) (
                        oldDamage
                                * (1.0D - reduction)
                );


        /*
         * ========================================================
         * 写回
         * ========================================================
         */

        event.setNewDamage(
                newDamage
        );
    }
}