package com.qidate.qisplan2.item;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.core.ModMobEffects;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.ghost.GhostPossessionManager;
import com.qidate.qisplan2.ghost.ItemGhostPossessionTarget;
import com.qidate.qisplan2.ghost.ability.divinationslip.GhostDivinationSlipAbility;
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
        if (!level.isClientSide) {

            int result =
                    level.random.nextInt(3);

            switch (result) {

                /*
                 * ====================================
                 * 生签
                 * ====================================
                 */
                case 0 -> {

                    MobEffectInstance existing =
                            player.getEffect(
                                    ModMobEffects
                                            .LIFE_SIGN_PROTECTION
                            );

                    int duration =
                            LIFE_SIGN_DURATION;

                    /*
                     * 已经有生签：
                     *
                     * 剩余时间 + 5分钟
                     */
                    if (existing != null) {

                        duration =
                                existing.getDuration()
                                        + LIFE_SIGN_DURATION;
                    }

                    player.addEffect(
                            new MobEffectInstance(
                                    ModMobEffects
                                            .LIFE_SIGN_PROTECTION,
                                    duration,
                                    4,      // V级
                                    false,
                                    false,
                                    true
                            )
                    );
                }

                /*
                 * ====================================
                 * 死签
                 * ====================================
                 */
                case 1 -> {

                    /*
                     * 如果当前正处于生签守护：
                     *
                     * 死签不会直接杀死玩家。
                     *
                     * 而是导致鬼签死机。
                     */
                    if (player.hasEffect(
                            ModMobEffects
                                    .LIFE_SIGN_PROTECTION
                    )) {

                        /*
                         * 移除生签守护。
                         */
                        player.removeEffect(
                                ModMobEffects
                                        .LIFE_SIGN_PROTECTION
                        );

                        /*
                         * 当前这个鬼签进入死机状态。
                         */
                        stack.set(
                                ModDataComponents.
                                        GHOST_DIVINATION_CRASHED_UNTIL,
                                level.getGameTime() + CRASH_DURATION
                        );

                        player.displayClientMessage(
                                Component.literal(
                                        "鬼签死机了。"
                                ),
                                true
                        );

                        /*
                         * 注意：
                         *
                         * 这里直接结束，
                         * 不执行正常死签袭击。
                         */
                        return InteractionResultHolder
                                .sidedSuccess(
                                        stack,
                                        level.isClientSide
                                );
                    }

                    /*
                     * 没有生签守护：
                     *
                     * 正常执行死签。
                     */
                    SupernaturalDeathHandler.tryKill(
                            player,
                            ModDamageTypes
                                    .ghostDivinationSlip(
                                            player
                                    ),
                            DEATH_SIGN_STRENGTH
                    );
                }

                /*
                 * ====================================
                 * 鬼签
                 * ====================================
                 */
                case 2 -> {

                    // 暂无效果
                }
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

            /*
             * ========================================
             * 通知客户端播放动画
             * ========================================
             */
            if (player instanceof ServerPlayer serverPlayer) {

                QisNetwork.sendGhostDivinationResult(
                        serverPlayer,
                        result
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }
}