package com.qidate.qisplan2.death;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.item.GhostShroudItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] ===== 开始检查鬼寿衣 ====="
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] 目标：{}",
                entity.getName().getString()
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] 灵异强度：{}",
                strength
        );

        /*
         * ========================================
         * 1. 是否是玩家
         * ========================================
         */
        if (!(entity instanceof ServerPlayer player)) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2][GhostShroud] 目标不是 ServerPlayer，保护结束"
            );

            return false;
        }

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] 确认目标是玩家：{}",
                player.getName().getString()
        );

        /*
         * ========================================
         * 2. 获取胸甲
         * ========================================
         */
        ItemStack chest =
                player.getItemBySlot(
                        EquipmentSlot.CHEST
                );

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] 胸甲物品：{}",
                chest.isEmpty()
                        ? "EMPTY"
                        : chest.getItem().toString()
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] 胸甲数量：{}",
                chest.getCount()
        );

        /*
         * ========================================
         * 3. 是否是鬼寿衣
         * ========================================
         */
        if (!(chest.getItem()
                instanceof GhostShroudItem)) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2][GhostShroud] 胸甲不是 GhostShroudItem，保护结束"
            );

            return false;
        }

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] ✅ 检测到鬼寿衣！"
        );

        /*
         * ========================================
         * 4. 获取最大生命属性
         * ========================================
         */
        AttributeInstance maxHealth =
                player.getAttribute(
                        Attributes.MAX_HEALTH
                );

        if (maxHealth == null) {

            QisPlan2.LOGGER.error(
                    "[QisPlan2][GhostShroud] ❌ 找不到 MAX_HEALTH 属性"
            );

            return false;
        }

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] 当前最大生命：{}",
                player.getMaxHealth()
        );

        /*
         * ========================================
         * 5. 检查本次消耗
         * ========================================
         */
        double cost =
                strength;

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] 本次生命上限消耗：{}",
                cost
        );

        /*
         * 至少保留 1 点最大生命。
         */
        if (player.getMaxHealth() - cost < 1.0D) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2][GhostShroud] ❌ 最大生命不足，无法抵挡"
            );

            return false;
        }

        /*
         * ========================================
         * 6. 获取当前鬼寿衣累计损耗
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

            QisPlan2.LOGGER.info(
                    "[QisPlan2][GhostShroud] 当前累计生命上限损失：{}",
                    currentLoss
            );

        } else {

            QisPlan2.LOGGER.info(
                    "[QisPlan2][GhostShroud] 当前没有生命上限损失 Modifier"
            );
        }

        /*
         * ========================================
         * 7. 计算新的损失
         * ========================================
         */
        double newLoss =
                currentLoss + cost;

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] 新的累计生命上限损失：{}",
                newLoss
        );

        /*
         * ========================================
         * 8. 添加属性 Modifier
         * ========================================
         */
        maxHealth.addOrReplacePermanentModifier(
                new AttributeModifier(
                        GHOST_SHROUD_MAX_HEALTH_LOSS,
                        -newLoss,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] ✅ MAX_HEALTH Modifier 已更新"
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] 新最大生命：{}",
                player.getMaxHealth()
        );

        /*
         * ========================================
         * 9. 当前生命不能超过新的上限
         * ========================================
         */
        if (player.getHealth()
                > player.getMaxHealth()) {

            QisPlan2.LOGGER.info(
                    "[QisPlan2][GhostShroud] 当前生命超过新上限，进行压制"
            );

            player.setHealth(
                    player.getMaxHealth()
            );
        }

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] ✅ 鬼寿衣成功抵挡灵异攻击"
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2][GhostShroud] ===== 检查结束 ====="
        );

        return true;
    }
}