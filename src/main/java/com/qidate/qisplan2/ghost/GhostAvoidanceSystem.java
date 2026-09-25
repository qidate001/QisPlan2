package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.item.RedGhostCandleItem;
import com.qidate.qisplan2.item.WhiteGhostCandleItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * 管理玩家与厉鬼之间的避鬼关系。
 *
 * <p>
 * 鬼烛是玩家侧的核心灵异机制。
 *
 * <p>
 * 白色鬼烛：
 * <ul>
 *     <li>阻止厉鬼发现玩家</li>
 *     <li>阻止厉鬼将玩家作为攻击目标</li>
 * </ul>
 *
 * <p>
 * 红色鬼烛：
 * <ul>
 *     <li>使玩家成为厉鬼的优先目标</li>
 * </ul>
 *
 * <p>
 * 本系统只负责提供统一的关系判定，
 * 不负责具体厉鬼的目标搜索与 AI 行为。
 */
public final class GhostAvoidanceSystem {

    private GhostAvoidanceSystem() {
    }

    /**
     * 判断玩家是否正在点燃白色鬼烛。
     */
    public static boolean hasWhiteGhostCandle(
            Player player
    ) {
        return hasLitCandle(
                player,
                WhiteGhostCandleItem.class
        );
    }

    /**
     * 判断玩家是否正在点燃红色鬼烛。
     */
    public static boolean hasRedGhostCandle(
            Player player
    ) {
        return hasLitCandle(
                player,
                RedGhostCandleItem.class
        );
    }

    /**
     * 判断厉鬼是否能够发现玩家。
     *
     * <p>
     * 点燃白色鬼烛时，玩家不会被厉鬼发现。
     */
    public static boolean canBeDetected(
            Player player
    ) {
        return !hasWhiteGhostCandle(player);
    }

    /**
     * 判断厉鬼是否能够攻击玩家。
     *
     * <p>
     * 点燃白色鬼烛时，玩家不会成为厉鬼的攻击目标。
     */
    public static boolean canBeAttacked(
            Player player
    ) {
        return !hasWhiteGhostCandle(player);
    }

    /**
     * 判断玩家是否应该成为厉鬼的优先目标。
     *
     * <p>
     * 点燃红色鬼烛后，玩家会获得吸引厉鬼的效果。
     */
    public static boolean shouldPrioritize(
            Player player
    ) {
        return hasRedGhostCandle(player);
    }

    /**
     * 判断玩家背包中是否存在指定类型的点燃鬼烛。
     */
    private static boolean hasLitCandle(
            Player player,
            Class<?> candleClass
    ) {
        for (ItemStack stack : player.getInventory().items) {
            if (!candleClass.isInstance(stack.getItem())) {
                continue;
            }

            if (isLit(stack)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断鬼烛是否处于点燃状态。
     */
    private static boolean isLit(
            ItemStack stack
    ) {
        CustomModelData customModelData =
                stack.get(DataComponents.CUSTOM_MODEL_DATA);

        return customModelData != null;
    }
}