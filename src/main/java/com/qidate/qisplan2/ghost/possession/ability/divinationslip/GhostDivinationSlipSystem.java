package com.qidate.qisplan2.ghost.possession.ability.divinationslip;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.curse.Curse;
import com.qidate.qisplan2.ghost.curse.CurseManager;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurse;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurseSource;
import com.qidate.qisplan2.ghost.curse.type.ghostdivination.GhostDivinationCurseType;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostState;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.network.divinationslip.GhostDivinationNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
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
     * 普通生签强度。
     */
    private static final int LIFE_STRENGTH =
            GhostDivinationCurse.DEFAULT_STRENGTH;

    /**
     * 死机状态下的生签强度。
     */
    private static final int LIFE_STRENGTH_STUN =
            GhostDivinationCurse.STUN_STRENGTH;

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

        GhostDivinationNetwork.sendResult(
                player,
                0
        );

        /*
         * 判断当前是否处于驾驭鬼签后的死机状态。
         */
        int strength =
                LIFE_STRENGTH;

        PossessedGhostState state =
                PossessionHandler.getState(
                        player,
                        GhostDivinationSlipAbility.ID
                );

        if (state != null
                && state.stunTicks() > 0) {

            strength =
                    LIFE_STRENGTH_STUN;
        }

        /*
         * 查找玩家当前已有的生签。
         */
        GhostDivinationCurse existing =
                getLifeCurse(
                        player.getUUID()
                );

        /*
         * 已经有生签：
         *
         * 只增加持续时间。
         *
         * 不重新创建诅咒，
         * 也不重置剩余时间。
         */
        if (existing != null) {

            existing.addTime(
                    LIFE_DURATION
            );

            /*
             * 如果当前不是死机状态，
             * 而重新抽到的生签强度应该恢复为普通强度，
             * 则同步更新强度。
             *
             * 死机状态下则保持 II 级强度。
             */
            existing.setStrength(
                    strength
            );

            CurseManager.markDirty();

            return;
        }

        /*
         * 第一次获得生签：
         *
         * 创建新的 Curse。
         */
        GhostDivinationCurseType curseType =
                new GhostDivinationCurseType();

        GhostDivinationCurseSource source =
                new GhostDivinationCurseSource();

        CompoundTag initialState =
                new CompoundTag();

        initialState.putInt(
                "Strength",
                strength
        );

        initialState.putInt(
                "RemainingTicks",
                LIFE_DURATION
        );

        Curse curse =
                curseType.create(
                        player.getUUID(),
                        source,
                        initialState
                );

        CurseManager.add(
                curse
        );
    }


    /*
     * ========================================
     * 查找生签
     * ========================================
     */

    /**
     * 获取玩家当前的生签诅咒。
     */
    public static GhostDivinationCurse getLifeCurse(
            java.util.UUID target
    ) {

        for (Curse curse :
                CurseManager.getByTarget(target)
        ) {

            if (curse instanceof GhostDivinationCurse lifeCurse) {
                return lifeCurse;
            }
        }

        return null;
    }


    /*
     * ========================================
     * 鬼签
     * ========================================
     */

    public static void useGhost(
            ServerPlayer player
    ) {

        GhostDivinationNetwork.sendResult(
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

        GhostDivinationNetwork.sendResult(
                player,
                1
        );

        /*
         * 生签保护中：
         *
         * 删除生签诅咒，
         * 然后进入普通死机。
         */
        GhostDivinationCurse lifeCurse =
                getLifeCurse(
                        player.getUUID()
                );

        if (lifeCurse != null) {

            CurseManager.remove(
                    lifeCurse.getId()
            );

            PossessionHandler.testStun(
                    player,
                    GhostDivinationSlipAbility.ID,
                    STUN_TIME
            );

            return;
        }

        /*
         * 没有生签：
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

        GhostDivinationNetwork.sendResult(
                player,
                1
        );

        GhostDivinationCurse lifeCurse =
                getLifeCurse(
                        player.getUUID()
                );

        /*
         * 生签保护中：
         *
         * 删除生签诅咒，
         * 然后写入物品 NBT 死机时间。
         */
        if (lifeCurse != null) {

            CurseManager.remove(
                    lifeCurse.getId()
            );

            stack.set(
                    ModDataComponents.GHOST_DIVINATION_CRASHED_UNTIL,
                    player.level().getGameTime()
                            + STUN_TIME
            );

            return;
        }

        /*
         * 没有生签：
         *
         * 正常死签。
         */
        SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostDivinationSlip(player),
                DEATH_STRENGTH
        );
    }
}