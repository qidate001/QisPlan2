package com.qidate.qisplan2.item;

import com.qidate.qisplan2.core.ModMobEffects;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.network.QisNetwork;
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

    public GhostDivinationSlipItem(Properties properties) {
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
                                    ModMobEffects.LIFE_SIGN_PROTECTION
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
                                    ModMobEffects.LIFE_SIGN_PROTECTION,
                                    duration,
                                    0,
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

                    SupernaturalDeathHandler.tryKill(
                            player,
                            ModDamageTypes.ghostDivinationSlip(
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
             * 消耗
             * ========================================
             */
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            /*
             * ========================================
             * 30秒冷却
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
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {

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