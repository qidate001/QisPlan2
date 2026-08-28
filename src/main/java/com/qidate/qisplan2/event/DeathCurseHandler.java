package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.item.DeathCurseSword;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;


@EventBusSubscriber(modid = QisPlan2.MODID)
public class DeathCurseHandler {

    private static final int MAX_CURSE_COUNT = 10;

    /**
     * 灵异袭击强度
     */
    private static final double SUPERNATURAL_ATTACK_STRENGTH = 3.0D;


    /**
     * 死亡诅咒之剑攻击事件
     */
    @SubscribeEvent
    public static void onLivingDamage(
            LivingDamageEvent.Pre event
    ) {

        // 攻击者必须是玩家
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        // 玩家主手必须拿着死亡诅咒之剑
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof DeathCurseSword)) {
            return;
        }

        // 本次攻击正常命中，但灵异实体不掉血
        if (event.getEntity() instanceof SupernaturalEntity) {
            event.setNewDamage(0.0F);
        } else {
            event.setNewDamage(1.0F);
        }

        LivingEntity target = event.getEntity();

        if (target.isDeadOrDying()) {
            return;
        }


        /*
         * ========================================
         * 被攻击者获得死亡诅咒
         * ========================================
         */

        int count =
                target.getData(ModAttachments.DEATH_CURSE_COUNT.get()) + 1;


        if (count >= MAX_CURSE_COUNT) {

            /*
             * 达到 10 层
             *
             * → 触发灵异即死
             */

            target.removeData(
                    ModAttachments.DEATH_CURSE_COUNT.get()
            );

            AttachmentSync.syncEntityUpdate(
                    target,
                    ModAttachments.DEATH_CURSE_COUNT.get()
            );

            breakDeathCurseSword(
                    player,
                    stack
            );

            player.sendSystemMessage(
                    Component.translatable(
                            "qisplan2.death_curse.triggered"
                    ).withStyle(
                            ChatFormatting.RED
                    )
            );

            SupernaturalDeathHandler.tryKill(
                    target,
                    ModDamageTypes.deathCurse(target),
                    SUPERNATURAL_ATTACK_STRENGTH
            );

        } else {

            target.setData(
                    ModAttachments.DEATH_CURSE_COUNT.get(),
                    count
            );

            AttachmentSync.syncEntityUpdate(
                    target,
                    ModAttachments.DEATH_CURSE_COUNT.get()
            );
        }


        /*
         * ========================================
         * 50% 概率反噬攻击者
         * ========================================
         */

        if (player.getRandom().nextFloat() < 0.5F) {

            int playerCurseCount =
                    player.getData(ModAttachments.DEATH_CURSE_COUNT.get()) + 1;


            if (playerCurseCount >= MAX_CURSE_COUNT) {

                /*
                 * 攻击者自己的诅咒达到 10 层
                 */

                player.removeData(
                        ModAttachments.DEATH_CURSE_COUNT.get()
                );

                AttachmentSync.syncEntityUpdate(
                        player,
                        ModAttachments.DEATH_CURSE_COUNT.get()
                );

                breakDeathCurseSword(
                        player,
                        stack
                );

                player.sendSystemMessage(
                        Component.translatable(
                                "qisplan2.death_curse.reflection"
                        ).withStyle(
                                ChatFormatting.DARK_GRAY
                        )
                );

                SupernaturalDeathHandler.tryKill(
                        player,
                        ModDamageTypes.deathCurse(player),
                        SUPERNATURAL_ATTACK_STRENGTH
                );

            } else {

                player.setData(
                        ModAttachments.DEATH_CURSE_COUNT.get(),
                        playerCurseCount
                );

                AttachmentSync.syncEntityUpdate(
                        player,
                        ModAttachments.DEATH_CURSE_COUNT.get()
                );

//                QisPlan2.LOGGER.info(
//                        "[QisPlan2] DeathCurse reflection synced, server player curse = {}",
//                        playerCurseCount
//                );

//                player.sendSystemMessage(
//                        Component.translatable(
//                                "qisplan2.death_curse.infected",
//                                playerCurseCount
//                        ).withStyle(
//                                ChatFormatting.DARK_GRAY
//                        )
//                );
            }
        }
    }


    /**
     * 玩家死亡 / 重生时清除死亡诅咒
     */
    @SubscribeEvent
    public static void onPlayerClone(
            PlayerEvent.Clone event
    ) {
        event.getEntity()
                .removeData(ModAttachments.DEATH_CURSE_COUNT.get());
    }


    /**
     * 死亡诅咒之剑损坏
     */
    private static void breakDeathCurseSword(
            Player player,
            ItemStack stack
    ) {

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


        // 生成物品破碎粒子
        if (player.level()
                instanceof ServerLevel serverLevel) {

            ItemParticleOption itemParticle =
                    new ItemParticleOption(
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


        // 从主手移除
        player.setItemSlot(
                EquipmentSlot.MAINHAND,
                ItemStack.EMPTY
        );
    }
}