package com.qidate.qisplan2.item.candle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public class WhiteGhostCandleSystem {

    private WhiteGhostCandleSystem() {
    }

    /**
     * 白鬼烛抵挡灵异袭击。
     *
     * @param player 玩家
     * @param intensity 本次灵异强度
     * @return 剩余灵异强度
     */
    public static double absorbAttack(
            ServerPlayer player,
            double intensity
    ) {

        if (intensity <= 0.0D) {
            return 0.0D;
        }

        ItemStack candle =
                findLitWhiteCandle(player);

        if (candle.isEmpty()) {
            return intensity;
        }

        int durabilityLeft =
                candle.getMaxDamage()
                        - candle.getDamageValue();

        if (durabilityLeft <= 0) {
            candle.shrink(1);
            return intensity;
        }

        /*
         * 1 灵异强度 = 10 耐久。
         */
        int requiredDurability =
                (int) Math.ceil(intensity * 10.0D);

        /*
         * 实际能够消耗多少耐久。
         */
        int consumed =
                Math.min(
                        durabilityLeft,
                        requiredDurability
                );

        candle.setDamageValue(
                candle.getDamageValue()
                        + consumed
        );

        if (candle.getDamageValue() >= candle.getMaxDamage()) {

            candle.shrink(1);
        }

        /*
         * 被抵挡掉的灵异强度。
         */
        double blockedIntensity =
                consumed / 10.0D;

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.CANDLE_EXTINGUISH,
                SoundSource.PLAYERS,
                0.7F,
                1.0F
        );

        return Math.max(
                0.0D,
                intensity - blockedIntensity
        );
    }

    private static ItemStack findLitWhiteCandle(
            ServerPlayer player
    ) {

        for (ItemStack stack : player.getInventory().items) {

            if (stack.getItem() instanceof WhiteGhostCandleItem
                    && WhiteGhostCandleItem.isLit(stack)) {

                return stack;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {

            if (stack.getItem() instanceof WhiteGhostCandleItem
                    && WhiteGhostCandleItem.isLit(stack)) {

                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
