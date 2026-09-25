package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.item.RedGhostCandleItem;
import com.qidate.qisplan2.item.WhiteGhostCandleItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * 管理厉鬼与玩家之间的基础灵异关系。
 *
 * <p>
 * 负责统一处理：
 *
 * <ul>
 *     <li>厉鬼是否能够发现玩家</li>
 *     <li>厉鬼是否能够攻击玩家</li>
 *     <li>玩家是否属于厉鬼的优先目标</li>
 * </ul>
 *
 * <p>
 * 这是实体厉鬼与玩家之间的底层规则系统。
 * 具体厉鬼的目标搜索、移动以及攻击行为，
 * 仍然由对应的厉鬼系统负责。
 */
public final class GhostAvoidanceSystem {

    private GhostAvoidanceSystem() {
    }

    /**
     * 判断厉鬼是否能够发现玩家。
     *
     * <p>
     * 点燃白色鬼烛时，玩家不会被厉鬼发现。
     *
     * @param ghost 厉鬼
     * @param player 玩家
     * @return 是否能够发现
     */
    public static boolean canDetect(
            AbstractGhostEntity ghost,
            Player player
    ) {
        if (hasWhiteGhostCandle(player)) {
            return false;
        }

        return true;
    }

    /**
     * 判断厉鬼是否能够攻击玩家。
     *
     * <p>
     * 能否攻击与能否发现是两个独立的规则。
     * 当前白色鬼烛同时阻止发现与攻击。
     *
     * @param ghost 厉鬼
     * @param player 玩家
     * @return 是否允许攻击
     */
    public static boolean canAttack(
            AbstractGhostEntity ghost,
            Player player
    ) {
        if (hasWhiteGhostCandle(player)) {
            return false;
        }

        return true;
    }

    /**
     * 获取玩家对于厉鬼的目标优先级。
     *
     * <p>
     * 数值越高，越应该优先选择该玩家。
     *
     * <p>
     * 红色鬼烛当前提供最高优先级。
     *
     * @param ghost 厉鬼
     * @param player 玩家
     * @return 目标优先级
     */
    public static int getTargetPriority(
            AbstractGhostEntity ghost,
            Player player
    ) {
        if (!canDetect(ghost, player)) {
            return Integer.MIN_VALUE;
        }

        if (hasRedGhostCandle(player)) {
            return 100;
        }

        return 0;
    }

    /**
     * 判断玩家是否点燃白色鬼烛。
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
     * 判断玩家是否点燃红色鬼烛。
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
     * 判断玩家身上是否存在指定类型的点燃鬼烛。
     *
     * <p>
     * 同时检查主背包与副手。
     */
    private static boolean hasLitCandle(
            Player player,
            Class<?> candleClass
    ) {
        for (ItemStack stack : player.getInventory().items) {
            if (candleClass.isInstance(stack.getItem())
                    && isLit(stack)) {
                return true;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (candleClass.isInstance(stack.getItem())
                    && isLit(stack)) {
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
        return stack.has(DataComponents.CUSTOM_MODEL_DATA);
    }
}