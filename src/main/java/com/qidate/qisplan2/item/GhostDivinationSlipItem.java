package com.qidate.qisplan2.item;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.core.ModMobEffects;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.GhostPossessionManager;
import com.qidate.qisplan2.ghost.ItemGhostPossessionTarget;
import com.qidate.qisplan2.ghost.ability.divinationslip.GhostDivinationSlipAbility;
import com.qidate.qisplan2.ghost.ability.divinationslip.GhostDivinationSlipSystem;
import com.qidate.qisplan2.network.QisNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GhostDivinationSlipItem extends Item {

    /**
     * 使用冷却：
     *
     * 30 秒 = 600 tick
     */
    private static final int COOLDOWN_TICKS = 30 * 20;
//    private static final int COOLDOWN_TICKS = 10;

    /**
     * 生签每次增加：
     *
     * 5 分钟 = 6000 tick
     */
    private static final int LIFE_SIGN_DURATION = 5 * 60 * 20;

    /**
     * 死签灵异袭击强度。
     */
    private static final double DEATH_SIGN_STRENGTH = 50.0D;

    /**
     * 鬼签死机时间：
     *
     * 5 分钟 = 6000 tick
     */
    private static final long CRASH_DURATION =
            5L * 60L * 20L;

    public GhostDivinationSlipItem(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        ItemStack stack =
                player.getItemInHand(hand);

        /*
         * ========================================
         * 死机检查
         * ========================================
         *
         * 死机状态属于当前这一个鬼签。
         */
        Long crashedUntil = stack.get(ModDataComponents.GHOST_DIVINATION_CRASHED_UNTIL);
        boolean crashed = crashedUntil != null
                && level.getGameTime() < crashedUntil;

        if (crashed) {
            // 死机状态：只有 Shift+右键才能尝试驾驭
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide()
                        && player instanceof ServerPlayer serverPlayer) {

                    boolean started =
                            GhostPossessionManager.start(
                                    serverPlayer,
                                    new ItemGhostPossessionTarget(
                                            stack,
                                            GhostDivinationSlipAbility.ID,
                                            targetPlayer -> stack.remove(
                                                    ModDataComponents
                                                            .GHOST_DIVINATION_CRASHED_UNTIL
                                            )
                                    )
                            );

                    if (started) {
                        return InteractionResultHolder.consume(stack);
                    }
                }

                return InteractionResultHolder.sidedSuccess(
                        stack,
                        level.isClientSide()
                );
            }

            // 死机状态下普通右键无效
            return InteractionResultHolder.fail(stack);
        }

        // 死机时间已经结束，清除状态
        if (crashedUntil != null) {
            stack.remove(ModDataComponents.GHOST_DIVINATION_CRASHED_UNTIL);
        }


        /*
         * ========================================
         * 冷却检查
         * ========================================
         */
        if (player.getCooldowns().isOnCooldown(this)) {

            return InteractionResultHolder.fail(
                    stack
            );
        }

        /*
         * ========================================
         * 服务端抽签
         * ========================================
         */
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {

            int result =
                    level.random.nextInt(3);

            switch (result) {

                case 0 ->
                        GhostDivinationSlipSystem.useLife(
                                serverPlayer
                        );

                case 1 ->
                        GhostDivinationSlipSystem.useDeathItem(
                                serverPlayer,
                                stack
                        );

                case 2 ->
                        GhostDivinationSlipSystem.useGhost(
                                serverPlayer
                        );
            }

            /*
             * ========================================
             * 30 tick 冷却
             * ========================================
             */
            player.getCooldowns().addCooldown(
                    this,
                    COOLDOWN_TICKS
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }
}