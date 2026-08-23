package com.qidate.qisplan2.item;

import com.qidate.qisplan2.ghost.PossessionHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class GhostWhitePorridgeItem extends Item {

    private static final double SHALLOW_STUN_AMOUNT =
            10.0D;

    public GhostWhitePorridgeItem(
            Properties properties
    ) {
        super(
                properties
                        .food(
                                new FoodProperties.Builder()
                                        .nutrition(4)
                                        .saturationModifier(0.3F)
                                        .build()
                        )
        );
    }

    @Override
    public int getUseDuration(
            ItemStack stack,
            LivingEntity entity
    ) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(
            ItemStack stack
    ) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(
            ItemStack stack,
            Level level,
            LivingEntity entity
    ) {
        ItemStack result =
                super.finishUsingItem(
                        stack,
                        level,
                        entity
                );

        /*
         * ========================================
         * 鬼白粥真正效果
         * ========================================
         */

        if (!level.isClientSide()
                && entity instanceof ServerPlayer player) {

            /*
             * 所有驾驭的厉鬼：
             * +10 浅死机值
             */
            int count =
                    PossessionHandler.addShallowStunToAll(
                            player,
                            SHALLOW_STUN_AMOUNT
                    );

            /*
             * 喝粥声音。
             */
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.GENERIC_DRINK,
                    SoundSource.PLAYERS,
                    1.0F,
                    0.9F
            );
        }

        /*
         * ========================================
         * 返回碗
         * ========================================
         *
         * 普通模式：
         * 粥被吃掉后返回一个碗。
         *
         * 创造模式：
         * stack 不会消耗，因此不额外返碗。
         */
        if (entity instanceof Player player
                && player.getAbilities().instabuild) {

            return result;
        }

        if (result.isEmpty()) {
            return new ItemStack(
                    Items.BOWL
            );
        }

        /*
         * 还有剩余粥时，
         * 把碗放入背包。
         */
        if (entity instanceof Player player) {

            ItemStack bowl =
                    new ItemStack(
                            Items.BOWL
                    );

            if (!player.getInventory()
                    .add(bowl)) {

                player.drop(
                        bowl,
                        false
                );
            }
        }

        return result;
    }
}