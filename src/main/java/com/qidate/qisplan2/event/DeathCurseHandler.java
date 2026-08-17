package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.item.DeathCurseSword;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.minecraft.core.particles.ItemParticleOption;

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
            // 触发死亡诅咒
            target.setHealth(0.0F);

            // 播放物品损坏音效
            player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ITEM_BREAK,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            // 生成死亡诅咒之剑的破碎粒子
            if (player.level() instanceof ServerLevel serverLevel) {

                ItemParticleOption itemParticle = new ItemParticleOption(
                        ParticleTypes.ITEM,
                        stack
                );

                serverLevel.sendParticles(
                        itemParticle,
                        player.getX(),
                        player.getY() + 1.0D,
                        player.getZ(),
                        10,
                        0.2D,
                        0.3D,
                        0.2D,
                        0.1D
                );
            }

            // 移除剑
            player.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    ItemStack.EMPTY
            );

            // 清除诅咒层数
            data.remove(CURSE_TAG);

            // 提示玩家
            player.sendSystemMessage(
                    Component.literal(
                            "§c☠ 死亡诅咒触发！死亡诅咒之剑已损坏！"
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