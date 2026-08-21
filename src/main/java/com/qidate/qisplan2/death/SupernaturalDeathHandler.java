package com.qidate.qisplan2.death;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.item.GhostShroudItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SupernaturalDeathHandler {


    private static final ResourceLocation
            GHOST_SHROUD_MAX_HEALTH_LOSS =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_shroud_max_health_loss"
            );


    /**
     * 基础灵异停滞时间：
     *
     * 100 tick = 5 秒
     *
     * 攻击强度 1 + 防御 1 时使用这个时间。
     */
    private static final int BASE_STUN_TIME = 100;

    /**
     * 最低停滞时间：
     *
     * 5 tick = 0.25 秒
     */
    private static final int MIN_STUN_TIME = 5;


    /**
     * 普通灵异攻击。
     *
     * 默认灵异强度 = 1。
     */
    public static boolean tryKill(
            LivingEntity entity,
            DamageSource damageSource
    ) {
        return tryKill(
                entity,
                damageSource,
                1.0D
        );
    }


    /**
     * 尝试执行一次灵异死亡。
     *
     * @param entity 目标
     * @param damageSource 灵异攻击伤害来源
     * @param supernaturalIntensity 灵异攻击强度
     *
     * @return true = 死亡成功
     *         false = 被抵消
     */
    public static boolean tryKill(
            LivingEntity entity,
            DamageSource damageSource,
            double supernaturalIntensity
    ) {
        QisPlan2.LOGGER.info(
                "[QisPlan2] tryKill 被调用：目标={}，强度={}",
                entity.getName().getString(),
                supernaturalIntensity
        );


        if (!entity.isAlive()) {
            return false;
        }

        // 防溢出
        supernaturalIntensity =
                Math.max(0.0D, supernaturalIntensity);

        // 鬼寿衣
        if (tryGhostShroudProtection(
                entity,
                supernaturalIntensity
        )) {


            QisPlan2.LOGGER.info(
                    "[QisPlan2] 鬼寿衣抵挡灵异攻击：{}，强度={}",
                    entity.getName().getString(),
                    supernaturalIntensity
            );
            return false;
        }

        /*
         * 灵异实体受到灵异攻击
         */
        if (entity instanceof SupernaturalEntity supernaturalEntity) {
            QisPlan2.LOGGER.info(
                    "[QisPlan2] 灵异攻击命中灵异实体：{}",
                    entity.getName().getString()
            );
            double defense =
                    Math.max(
                            0.01D,
                            supernaturalEntity.getSupernaturalDefense()
                    );

            double stunTicksDouble =
                    BASE_STUN_TIME
                            * supernaturalIntensity
                            / defense;

            int stunTicks =
                    Math.max(
                            MIN_STUN_TIME,
                            (int) Math.round(stunTicksDouble)
                    );

            supernaturalEntity.onSupernaturalAttack(
                    stunTicks
            );

            return false;
        }

        /*
         * 普通实体仍然是秒杀逻辑
         */
        entity.hurt(
                damageSource,
                Float.MAX_VALUE
        );

        boolean instantlyKill =
                entity.level()
                        .getGameRules()
                        .getRule(
                                QisPlan2.GHOST_DAMAGE_INSTANTLY_KILL
                        )
                        .get();

        if (entity instanceof Player player
                && player.isCreative()
                && !instantlyKill) {

            return false;
        }

        if (instantlyKill) {
            entity.setHealth(0.0F);
            return true;
        }

        return !entity.isAlive();
    }

    private static boolean tryGhostShroudProtection(
            LivingEntity entity,
            double strength
    ) {
        /*
         * 必须是玩家。
         */
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }

        /*
         * 必须穿着鬼寿衣。
         */
        ItemStack chest =
                player.getItemBySlot(
                        EquipmentSlot.CHEST
                );

        if (!(chest.getItem() instanceof GhostShroudItem)) {
            return false;
        }

        /*
         * 获取最大生命属性。
         */
        AttributeInstance maxHealth =
                player.getAttribute(
                        Attributes.MAX_HEALTH
                );

        if (maxHealth == null) {
            return false;
        }

        /*
         * ========================================
         * 计算本次最大生命消耗
         * ========================================
         *
         * 最低消耗 1 点。
         */
        double cost =
                Math.max(
                        1.0D,
                        strength
                );

        /*
         * 至少保留 1 点最大生命。
         */
        if (player.getMaxHealth() - cost < 1.0D) {
            return false;
        }

        /*
         * ========================================
         * 获取鬼寿衣此前累计造成的最大生命损失
         * ========================================
         */
        double currentLoss = 0.0D;

        AttributeModifier modifier =
                maxHealth.getModifier(
                        GHOST_SHROUD_MAX_HEALTH_LOSS
                );

        if (modifier != null) {
            currentLoss =
                    -modifier.amount();
        }

        /*
         * 累加本次消耗。
         */
        double newLoss =
                currentLoss + cost;

        /*
         * ========================================
         * 永久降低最大生命
         * ========================================
         */
        maxHealth.addOrReplacePermanentModifier(
                new AttributeModifier(
                        GHOST_SHROUD_MAX_HEALTH_LOSS,
                        -newLoss,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );

        /*
         * 最大生命下降后，
         * 当前生命不能超过新的上限。
         */
        if (player.getHealth()
                > player.getMaxHealth()) {

            player.setHealth(
                    player.getMaxHealth()
            );
        }

        /*
         * 鬼寿衣成功抵挡的音效。
         */
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        return true;
    }
}