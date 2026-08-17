package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.item.DeathCurseSword;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class DeathCurseHandler {

    private static final String CURSE_TAG = "death_curse_count";

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {

        // 检查攻击者是否为玩家
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        // 检查玩家主手是否拿着死亡诅咒之剑
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof DeathCurseSword)) {
            return;
        }

        // 强制本次伤害为 1
        event.setNewDamage(1.0F);

        // 获取被攻击的实体
        LivingEntity target = event.getEntity();

        if (target.isDeadOrDying()) {
            return;
        }

        // 获取诅咒层数
        CompoundTag data = target.getPersistentData();

        int count = data.getInt(CURSE_TAG);
        count++;

        if (count >= 10) {

            // 第 10 次攻击：触发死亡诅咒
            target.setHealth(0.0F);

            // 剑立即损坏并从主手消失
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

            // 清除诅咒层数
            data.remove(CURSE_TAG);

            // 提示玩家
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§c☠ 死亡诅咒触发！剑已破损！"
                    )
            );

        } else {

            // 保存新的诅咒层数
            data.putInt(CURSE_TAG, count);

            // 提示玩家
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            String.format("§e诅咒层数: %d/10", count)
                    )
            );
        }
    }
}