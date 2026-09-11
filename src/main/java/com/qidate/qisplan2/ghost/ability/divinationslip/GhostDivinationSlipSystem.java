package com.qidate.qisplan2.ghost.ability.divinationslip;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.core.ModMobEffects;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.PossessedGhostState;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.network.QisNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

public final class GhostDivinationSlipSystem {

    private GhostDivinationSlipSystem() {
    }

    /*
     * ========================================
     * 常量
     * ========================================
     */

    /**
     * 生签持续时间。
     */
    private static final int LIFE_DURATION =
            5 * 60 * 20;

    /**
     * V级守护。
     */
    private static final int LIFE_LEVEL_NORMAL = 4;

    /**
     * II级守护。
     */
    private static final int LIFE_LEVEL_STUN = 1;

    /**
     * 普通死机时间。
     */
    private static final long STUN_TIME =
            5L * 60L * 20L;

    /**
     * 死签灵异强度。
     */
    private static final double DEATH_STRENGTH =
            50.0D;


    /*
     * ========================================
     * 生签
     * ========================================
     */

    public static void useLife(
            ServerPlayer player
    ) {

        QisNetwork.sendGhostDivinationResult(
                player,
                0
        );

        MobEffectInstance existing =
                player.getEffect(
                        ModMobEffects.LIFE_SIGN_PROTECTION
                );

        int duration =
                existing == null
                        ? LIFE_DURATION
                        : existing.getDuration()
                        + LIFE_DURATION;

        /*
         * 判断是否已经驾驭鬼签，
         * 且当前是否普通死机。
         */
        int amplifier =
                LIFE_LEVEL_NORMAL;

        PossessedGhostState state =
                PossessionHandler.getState(
                        player,
                        GhostDivinationSlipAbility.ID
                );

        if (state != null
                && state.stunTicks() > 0) {

            amplifier =
                    LIFE_LEVEL_STUN;
        }

        player.addEffect(
                new MobEffectInstance(
                        ModMobEffects.LIFE_SIGN_PROTECTION,
                        duration,
                        amplifier,
                        false,
                        false,
                        true
                )
        );
    }


    /*
     * ========================================
     * 鬼签
     * ========================================
     */

    public static void useGhost(
            ServerPlayer player
    ) {

        QisNetwork.sendGhostDivinationResult(
                player,
                2
        );

        // 暂无效果
    }


    /*
     * ========================================
     * 死签（驾驭）
     * ========================================
     */

    public static void useDeath(
            ServerPlayer player
    ) {

        QisNetwork.sendGhostDivinationResult(
                player,
                1
        );

        /*
         * 生签保护中：
         *
         * 鬼签普通死机。
         */
        if (player.hasEffect(
                ModMobEffects.LIFE_SIGN_PROTECTION
        )) {

            player.removeEffect(
                    ModMobEffects.LIFE_SIGN_PROTECTION
            );

            PossessionHandler.testStun(
                    player,
                    GhostDivinationSlipAbility.ID,
                    STUN_TIME
            );

            return;
        }

        /*
         * 没保护：
         *
         * 正常死签。
         */
        SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostDivinationSlip(player),
                DEATH_STRENGTH
        );
    }


    /*
     * ========================================
     * 死签（物品）
     * ========================================
     *
     * 普通鬼签仍然走物品 NBT 死机。
     */

    public static void useDeathItem(
            ServerPlayer player,
            ItemStack stack
    ) {

        QisNetwork.sendGhostDivinationResult(
                player,
                1
        );

        if (player.hasEffect(
                ModMobEffects.LIFE_SIGN_PROTECTION
        )) {

            player.removeEffect(
                    ModMobEffects.LIFE_SIGN_PROTECTION
            );

            stack.set(
                    ModDataComponents.GHOST_DIVINATION_CRASHED_UNTIL,
                    player.level().getGameTime()
                            + STUN_TIME
            );

            return;
        }

        SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostDivinationSlip(player),
                DEATH_STRENGTH
        );
    }
}