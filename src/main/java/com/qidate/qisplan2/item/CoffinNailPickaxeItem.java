package com.qidate.qisplan2.item;

import com.qidate.qisplan2.core.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;

import java.util.function.Consumer;

public class CoffinNailPickaxeItem extends PickaxeItem {

    public CoffinNailPickaxeItem(
            Properties properties,
            Tier tier
    ) {
        super(tier, properties);
    }

    @Override
    public <T extends LivingEntity> int damageItem(
            ItemStack stack,
            int amount,
            T entity,
            Consumer<Item> onBroken
    ) {
        if (amount <= 0) {
            return 0;
        }

        int currentDamage = stack.getDamageValue();
        int maxDamage = stack.getMaxDamage();

        /*
         * 正常损耗还没有到极限
         */
        if (currentDamage + amount < maxDamage) {
            return amount;
        }

        /*
         * 即将损坏：
         *
         * 1. 触发原版破损回调
         * 2. 返还一个棺材钉
         * 3. 删除当前镐子
         * 4. 阻止原版继续处理耐久
         */

        if (entity instanceof ServerPlayer player) {

            // 原版物品破损效果
            onBroken.accept(this);

            // 返还棺材钉
            ItemStack coffinNail =
                    new ItemStack(ModItems.COFFIN_NAIL.get());

            if (!player.getInventory().add(coffinNail)) {
                // 背包满了就掉到玩家脚边
                player.drop(coffinNail, false);
            }

            // 删除已经损坏的棺材钉镐子
            stack.shrink(1);

            // 给原版统计数据留出正常的破损流程
            player.awardStat(
                    net.minecraft.stats.Stats.ITEM_BROKEN.get(this)
            );
        }

        /*
         * 返回 0：
         * 告诉 vanilla 不要再对这个 stack 继续施加耐久损耗。
         *
         * 因为我们已经自己处理“损坏并消失”了。
         */
        return 0;
    }
}